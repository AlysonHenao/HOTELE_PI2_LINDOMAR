package co.lindomar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.lindomar.domain.Domain.*;
import co.lindomar.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verifica integridad del balance, permisos y reintentos de pagos del Sprint 3. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:sprint3;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class FinanceSprint3IntegrationTest {
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2030-06-10T16:00:00Z"), ZoneId.of("America/Bogota"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FinanceRepository finance;
  @Autowired ReminderRepository reminders;
  @Autowired RoomRepository rooms;
  @Autowired ReservationRepository reservations;
  @Autowired TaskRepository tasks;
  @Autowired Clock clock;

  private String auth(String email) throws Exception {
    return "Bearer "
        + body(mvc.perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(Map.of("email", email, "password", "demo123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
            .path("token")
            .asText();
  }

  private JsonNode body(String response) throws Exception {
    return json.readTree(response);
  }

  private ExpenseReminder reminder() {
    return reminders.saveAndFlush(
        new ExpenseReminder(
            "S3-" + UUID.randomUUID(),
            "Servicios",
            new BigDecimal("25000.50"),
            LocalDate.now(clock).plusDays(2),
            ReminderStatus.PENDING));
  }

  private JsonNode pay(Long id, String token) throws Exception {
    return body(
        mvc.perform(post("/api/reminders/{id}/pay", id).header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "0", "0.001", "1000000000000", "null"})
  void invalidAmountsDoNotChangeBalance(String amount) throws Exception {
    long before = finance.count();
    mvc.perform(
            post("/api/finance")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"concept\":\"Agua\",\"amount\":" + amount + "}"))
        .andExpect(status().isBadRequest());
    assertThat(finance.count()).isEqualTo(before);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"concept\":\"Agua\",\"amount\":10}",
        "{\"type\":\"INCOME\",\"concept\":\"   \",\"amount\":10}",
        "{\"type\":\"OTHER\",\"concept\":\"Agua\",\"amount\":10}",
        "{\"type\":\"INCOME\",\"concept\":\"Agua\",\"amount\":10,\"date\":\"2030-06-11\"}",
        "{\"type\":\"INCOME\",\"concept\":\"Agua\",\"amount\":10,\"date\":\"no-fecha\"}"
      })
  void invalidMovementsAreRejected(String request) throws Exception {
    long before = finance.count();
    mvc.perform(
            post("/api/finance")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest());
    assertThat(finance.count()).isEqualTo(before);
  }

  @Test
  void validIncomeAndExpenseUseNewIdsAndReconcileExactly() throws Exception {
    var admin = auth("admin@lindomar.co");
    var before =
        body(mvc.perform(get("/api/dashboard").header("Authorization", admin))
                .andReturn()
                .getResponse()
                .getContentAsString())
            .path("balance")
            .decimalValue();
    var existing = finance.findAll().getFirst();
    var original = existing.getAmount();
    for (var type : List.of("INCOME", "EXPENSE")) {
      var result =
          body(
              mvc.perform(
                      post("/api/finance")
                          .header("Authorization", admin)
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(
                              json.writeValueAsString(
                                  Map.of(
                                      "id",
                                      existing.getId(),
                                      "type",
                                      type,
                                      "concept",
                                      "  Ajuste  ",
                                      "amount",
                                      type.equals("INCOME") ? "100.50" : "25.25"))))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.concept").value("Ajuste"))
                  .andExpect(jsonPath("$.date").value("2030-06-10"))
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      assertThat(result.path("id").asLong()).isNotEqualTo(existing.getId());
    }
    assertThat(finance.findById(existing.getId()).orElseThrow().getAmount())
        .isEqualByComparingTo(original);
    var after =
        body(mvc.perform(get("/api/dashboard").header("Authorization", admin))
                .andReturn()
                .getResponse()
                .getContentAsString())
            .path("balance")
            .decimalValue();
    assertThat(after.subtract(before)).isEqualByComparingTo("75.25");
  }

  @ParameterizedTest
  @ValueSource(strings = {"huesped@lindomar.co", "empleado@lindomar.co"})
  void financialOperationsRequireAdmin(String email) throws Exception {
    var token = auth(email);
    var r = reminder();
    mvc.perform(
            post("/api/finance")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"concept\":\"Agua\",\"amount\":10}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/reminders/{id}/pay", r.getId()).header("Authorization", token))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/finance").header("Authorization", token))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/reminders/{id}", r.getId()).header("Authorization", token))
        .andExpect(status().isForbidden());
    assertThat(reminders.findById(r.getId()).orElseThrow().getStatus())
        .isEqualTo(ReminderStatus.PENDING);
  }

  @Test
  void repeatedPaymentReturnsSameMovementAndPreservesAuditTrail() throws Exception {
    var r = reminder();
    var admin = auth("admin@lindomar.co");
    long before = finance.count();
    var one = pay(r.getId(), admin);
    var two = pay(r.getId(), admin);
    assertThat(two.path("id")).isEqualTo(one.path("id"));
    assertThat(finance.count()).isEqualTo(before + 1);
    assertThat(one.path("amount").decimalValue()).isEqualByComparingTo("25000.50");
    assertThat(reminders.findById(r.getId()).orElseThrow().getPaymentEntryId())
        .isEqualTo(one.path("id").asLong());
    mvc.perform(
            put("/api/reminders/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PENDING\"}"))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/reminders/{id}", r.getId()).header("Authorization", admin))
        .andExpect(status().isConflict());
  }

  @Test
  void invalidReminderAndDirectDoneCannotBypassPayment() throws Exception {
    var admin = auth("admin@lindomar.co");
    long before = reminders.count();
    for (var amount : List.of("-1", "0", "0.001", "null")) {
      mvc.perform(
              post("/api/reminders")
                  .header("Authorization", admin)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"concept\":\"Agua\",\"category\":\"Servicios\",\"dueDate\":\"2030-06-11\","
                          + "\"estimatedAmount\":"
                          + amount
                          + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/api/reminders")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"concept\":\"Agua\",\"category\":\"Servicios\",\"estimatedAmount\":1}"))
        .andExpect(status().isBadRequest());
    assertThat(reminders.count()).isEqualTo(before);
    var r = reminder();
    mvc.perform(
            put("/api/reminders/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void createIgnoresClientPaymentIdentityAndPendingReminderCanBeEdited() throws Exception {
    var admin = auth("admin@lindomar.co");
    var result =
        body(
            mvc.perform(
                    post("/api/reminders")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"id\":1,\"paymentEntryId\":1,\"status\":\"DONE\",\"concept\":\" Agua \","
                                + "\"category\":\" Servicios \",\"estimatedAmount\":50,\"dueDate\":\"2030-06-11\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    long id = result.path("id").asLong();
    mvc.perform(
            put("/api/reminders/{id}", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estimatedAmount\":75.50}"))
        .andExpect(status().isOk());
    assertThat(pay(id, admin).path("amount").decimalValue()).isEqualByComparingTo("75.50");
  }

  @Test
  void legacyDoneAndInvalidLegacyReminderDoNotGenerateAnotherExpense() throws Exception {
    var admin = auth("admin@lindomar.co");
    var r = reminder();
    r.setStatus(ReminderStatus.DONE);
    reminders.saveAndFlush(r);
    long before = finance.count();
    mvc.perform(post("/api/reminders/{id}/pay", r.getId()).header("Authorization", admin))
        .andExpect(status().isConflict());
    var invalid = reminder();
    invalid.setEstimatedAmount(BigDecimal.ZERO);
    reminders.saveAndFlush(invalid);
    mvc.perform(post("/api/reminders/{id}/pay", invalid.getId()).header("Authorization", admin))
        .andExpect(status().isConflict());
    assertThat(finance.count()).isEqualTo(before);
  }

  @Test
  void nonexistentReminderReturns404AndAnonymousCannotPay() throws Exception {
    mvc.perform(
            post("/api/reminders/999999999/pay").header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isNotFound());
    mvc.perform(post("/api/reminders/1/pay")).andExpect(status().isUnauthorized());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousPaymentsGenerateExactlyOneExpense() throws Exception {
    var r = reminder();
    var admin = auth("admin@lindomar.co");
    Long entryId = null;
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Long> request =
          () -> {
            start.await();
            return pay(r.getId(), admin).path("id").asLong();
          };
      var one = pool.submit(request);
      var two = pool.submit(request);
      start.countDown();
      entryId = one.get(15, TimeUnit.SECONDS);
      assertThat(two.get(15, TimeUnit.SECONDS)).isEqualTo(entryId);
      assertThat(
              finance.findAll().stream().filter(e -> e.getConcept().equals(r.getConcept())).count())
          .isEqualTo(1);
    } finally {
      reminders.deleteById(r.getId());
      if (entryId != null) finance.deleteById(entryId);
    }
  }

  @Test
  void oversizedReservationNotesReturn400WithoutCreatingIncomeOrTasks() throws Exception {
    long beforeFinance = finance.count();
    long beforeTasks = tasks.count();
    var room =
        rooms.findAll().stream()
            .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
            .findFirst()
            .orElseThrow();
    mvc.perform(
            post("/api/reservations")
                .header("Authorization", auth("huesped@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "roomId",
                            room.getId(),
                            "checkIn",
                            "2030-06-15",
                            "checkOut",
                            "2030-06-17",
                            "guests",
                            1,
                            "notes",
                            "x".repeat(256)))))
        .andExpect(status().isBadRequest());
    assertThat(finance.count()).isEqualTo(beforeFinance);
    assertThat(tasks.count()).isEqualTo(beforeTasks);
  }
}
