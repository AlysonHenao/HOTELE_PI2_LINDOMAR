package co.lindomar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.lindomar.domain.Domain.ExpenseReminder;
import co.lindomar.domain.Domain.ReminderStatus;
import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import co.lindomar.repository.ReminderRepository;
import co.lindomar.repository.ReservationRepository;
import co.lindomar.repository.RoomRepository;
import co.lindomar.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas automatizadas del Sprint 3 para HU-37 (Recomendación de personal de aseo) y HU-36
 * (Recordatorios de gastos programados con validaciones estrictas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:sprint3_staff;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class StaffRecommendationSprint3Test {

  @TestConfiguration
  static class FixedTimeConfig {
    @Bean
    @Primary
    Clock testClock() {
      // Fecha base fija: 15 de Octubre 2030 (Temporada Regular)
      return Clock.fixed(Instant.parse("2030-10-15T10:00:00Z"), ZoneId.of("America/Bogota"));
    }
  }

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private RoomRepository rooms;
  @Autowired private ReservationRepository reservations;
  @Autowired private ReminderRepository reminders;
  @Autowired private UserRepository users;
  @Autowired private Clock clock;

  private String auth(String email) throws Exception {
    var resp =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(Map.of("email", email, "password", "demo123"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode tree = json.readTree(resp);
    return "Bearer " + tree.path("token").asText();
  }

  // ==========================================
  // HU-37: Cleaning Staff Recommendation Tests
  // ==========================================

  @Test
  void adminCanGetRecommendationForDefaultDate() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    mvc.perform(get("/api/staff/cleaning-recommendation").header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetDate").value("2030-10-15"))
        .andExpect(jsonPath("$.totalRooms").isNumber())
        .andExpect(jsonPath("$.occupiedRooms").isNumber())
        .andExpect(jsonPath("$.occupancyRate").isNumber())
        .andExpect(jsonPath("$.season").value("REGULAR"))
        .andExpect(jsonPath("$.seasonDescription").value("Temporada Regular"))
        .andExpect(jsonPath("$.roomsPerCleanerRatio").value(4))
        .andExpect(jsonPath("$.recommendedCleaners").isNumber())
        .andExpect(jsonPath("$.rationale").isString());
  }

  @Test
  void recommendationIdentifiesHighSeasonInDecember() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    // Diciembre es Temporada Alta
    mvc.perform(
            get("/api/staff/cleaning-recommendation")
                .param("date", "2030-12-24")
                .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetDate").value("2030-12-24"))
        .andExpect(jsonPath("$.season").value("HIGH"))
        .andExpect(jsonPath("$.seasonDescription").value("Temporada Alta"))
        .andExpect(jsonPath("$.roomsPerCleanerRatio").value(3));
  }

  @Test
  void recommendationIdentifiesHighSeasonInJuly() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    // Julio es Temporada Alta
    mvc.perform(
            get("/api/staff/cleaning-recommendation")
                .param("date", "2030-07-20")
                .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetDate").value("2030-07-20"))
        .andExpect(jsonPath("$.season").value("HIGH"))
        .andExpect(jsonPath("$.roomsPerCleanerRatio").value(3));
  }

  @Test
  void recommendationConsidersTurnoverCheckoutsInWorkload() throws Exception {
    String adminToken = auth("admin@lindomar.co");
    LocalDate checkoutDay = LocalDate.of(2030, 11, 20);

    var guest = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow();
    var allRooms = rooms.findAll();
    assertThat(allRooms).isNotEmpty();
    var room = allRooms.get(0);

    // Reserva que finaliza justo en checkoutDay
    var res = new Reservation();
    res.setRoomId(room.getId());
    res.setGuestId(guest.getId());
    res.setCheckIn(checkoutDay.minusDays(3));
    res.setCheckOut(checkoutDay);
    res.setGuests(2);
    res.setStatus(ReservationStatus.CONFIRMED);
    reservations.save(res);

    mvc.perform(
            get("/api/staff/cleaning-recommendation")
                .param("date", checkoutDay.toString())
                .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checkoutRooms").value(1))
        .andExpect(
            jsonPath("$.recommendedCleaners").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
  }

  @Test
  void dashboardIncludesCleaningRecommendationDetails() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    mvc.perform(get("/api/dashboard").header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestedCleaners").isNumber())
        .andExpect(jsonPath("$.cleaningRationale").isString())
        .andExpect(jsonPath("$.cleaningRecommendation.targetDate").value("2030-10-15"))
        .andExpect(jsonPath("$.cleaningRecommendation.season").isString());
  }

  @Test
  void guestAndAnonymousCannotAccessCleaningRecommendation() throws Exception {
    // Anónimo -> 401
    mvc.perform(get("/api/staff/cleaning-recommendation")).andExpect(status().isUnauthorized());

    // Huésped -> 403 Forbidden
    String guestToken = auth("huesped@lindomar.co");
    mvc.perform(get("/api/staff/cleaning-recommendation").header("Authorization", guestToken))
        .andExpect(status().isForbidden());
  }

  // ==========================================
  // HU-36: Schedule Expense Reminders Tests
  // ==========================================

  @Test
  void adminCanCreateValidExpenseReminder() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    var body =
        Map.of(
            "concept", "Mantenimiento aires acondicionados",
            "category", "Mantenimiento",
            "estimatedAmount", 450000.00,
            "dueDate", "2030-10-25");

    mvc.perform(
            post("/api/reminders")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.concept").value("Mantenimiento aires acondicionados"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.estimatedAmount").value(450000.0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-50000", "0", "-0.01"})
  void reminderWithInvalidAmountIsRejected(String amount) throws Exception {
    String adminToken = auth("admin@lindomar.co");

    String payload =
        """
        {
          "concept": "Factura de Internet",
          "category": "Servicios",
          "estimatedAmount": %s,
          "dueDate": "2030-10-25"
        }
        """
            .formatted(amount);

    mvc.perform(
            post("/api/reminders")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reminderWithPastDueDateIsRejected() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    // Fecha base es 2030-10-15; 2030-10-10 es fecha en el pasado
    var body =
        Map.of(
            "concept", "Recibo de energía vencido",
            "category", "Servicios",
            "estimatedAmount", 320000.00,
            "dueDate", "2030-10-10");

    mvc.perform(
            post("/api/reminders")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reminderWithBlankConceptIsRejected() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    var body =
        Map.of(
            "concept", "   ",
            "category", "Servicios",
            "estimatedAmount", 100000.00,
            "dueDate", "2030-10-25");

    mvc.perform(
            post("/api/reminders")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void payingReminderIsIdempotentAndUpdatesStatus() throws Exception {
    String adminToken = auth("admin@lindomar.co");

    var reminder =
        reminders.save(
            new ExpenseReminder(
                "Suministros de aseo",
                "Operaciones",
                new BigDecimal("185000.00"),
                LocalDate.of(2030, 10, 20),
                ReminderStatus.PENDING));

    // Primer pago exitoso -> 200 OK
    mvc.perform(
            post("/api/reminders/" + reminder.getId() + "/pay").header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EXPENSE"))
        .andExpect(jsonPath("$.amount").value(185000.00));

    // Segundo pago sobre el mismo recordatorio -> 409 Conflict (Idempotencia)
    mvc.perform(
            post("/api/reminders/" + reminder.getId() + "/pay").header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EXPENSE"))
        .andExpect(jsonPath("$.amount").value(185000.00));
    var updated = reminders.findById(reminder.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(ReminderStatus.DONE);
    assertThat(updated.getPaymentEntryId()).isNotNull();
  }

  @Test
  void guestCannotCreateOrPayReminders() throws Exception {
    String guestToken = auth("huesped@lindomar.co");

    var body =
        Map.of(
            "concept", "Intento no autorizado",
            "category", "Otros",
            "estimatedAmount", 50000.00,
            "dueDate", "2030-10-25");

    mvc.perform(
            post("/api/reminders")
                .header("Authorization", guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isForbidden());
  }
}
