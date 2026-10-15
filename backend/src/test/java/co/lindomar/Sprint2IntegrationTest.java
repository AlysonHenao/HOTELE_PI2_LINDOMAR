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
import org.junit.jupiter.params.provider.EnumSource;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:sprint2;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class Sprint2IntegrationTest {
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
  @Autowired UserRepository users;
  @Autowired RoomRepository rooms;
  @Autowired ReservationRepository reservations;
  @Autowired FinanceRepository finance;
  @Autowired TaskRepository tasks;
  @Autowired AuthSessionRepository sessions;
  @Autowired Clock clock;

  private String auth(String email) throws Exception {
    return "Bearer "
        + json.readTree(
                mvc.perform(
                        post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                json.writeValueAsString(
                                    Map.of("email", email, "password", "demo123"))))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
            .get("token")
            .asText();
  }

  private Room room() {
    var room =
        new Room(
            "S2-" + UUID.randomUUID(),
            3,
            "Suite",
            2,
            new BigDecimal("150000"),
            true,
            false,
            "Wi-Fi · TV",
            RoomStatus.AVAILABLE);
    room.setImageUrl("https://example.com/room.jpg");
    return rooms.saveAndFlush(room);
  }

  private Reservation reservation(Room room, int days, ReservationStatus status) {
    var r = new Reservation();
    r.setRoomId(room.getId());
    r.setGuestId(users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow().getId());
    r.setCheckIn(LocalDate.now(clock).plusDays(days));
    r.setCheckOut(r.getCheckIn().plusDays(2));
    r.setGuests(1);
    r.setAmount(new BigDecimal("300000"));
    r.setStatus(status);
    return reservations.saveAndFlush(r);
  }

  private JsonNode detail(Room room, int days) throws Exception {
    return json.readTree(
        mvc.perform(
                get("/api/rooms/{id}", room.getId())
                    .param("checkIn", LocalDate.now(clock).plusDays(days).toString())
                    .param("checkOut", LocalDate.now(clock).plusDays(days + 2).toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  private long refunds(Long id) {
    return finance.findAll().stream()
        .filter(
            e ->
                e.getType() == EntryType.EXPENSE
                    && e.getConcept().startsWith("Reembolso reserva #" + id + " ·"))
        .count();
  }

  @Test
  void roomDetailsContainCurrentInformationAndPeriodPrice() throws Exception {
    var room = room();
    var result = detail(room, 5);
    assertThat(result.path("room").path("id").asLong()).isEqualTo(room.getId());
    assertThat(result.path("room").path("imageUrl").asText()).isEqualTo(room.getImageUrl());
    assertThat(result.path("room").path("floor").asInt()).isEqualTo(3);
    assertThat(result.path("room").path("services").asText()).isEqualTo("Wi-Fi · TV");
    assertThat(result.path("room").path("capacity").asInt()).isEqualTo(2);
    assertThat(result.path("room").path("balcony").asBoolean()).isTrue();
    assertThat(result.path("room").path("petFriendly").asBoolean()).isFalse();
    assertThat(result.path("available").asBoolean()).isTrue();
    assertThat(result.path("total").decimalValue()).isEqualByComparingTo("300000");
    assertThat(result.path("nights").asLong()).isEqualTo(2);
    reservation(room, 5, ReservationStatus.CONFIRMED);
    assertThat(detail(room, 5).path("available").asBoolean()).isFalse();
    assertThat(detail(room, 7).path("available").asBoolean()).isTrue();
    room.setPrice(new BigDecimal("175000"));
    rooms.saveAndFlush(room);
    assertThat(detail(room, 7).path("total").decimalValue()).isEqualByComparingTo("350000");
  }

  @ParameterizedTest
  @EnumSource(RoomStatus.class)
  void detailsRespectEveryOperationalStatus(RoomStatus state) throws Exception {
    var room = room();
    room.setStatus(state);
    rooms.saveAndFlush(room);
    assertThat(detail(room, 5).path("available").asBoolean())
        .isEqualTo(state == RoomStatus.AVAILABLE);
  }

  @Test
  void missingRoomAndInvalidDatesHaveExplicitErrors() throws Exception {
    var room = room();
    mvc.perform(get("/api/rooms/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    mvc.perform(get("/api/rooms/{id}", room.getId()).param("checkIn", "2030-06-15"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/rooms/{id}", room.getId())
                .param("checkIn", "bad")
                .param("checkOut", "2030-06-16"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/rooms/{id}", room.getId())
                .param("checkIn", "2030-06-15")
                .param("checkOut", "2030-06-15"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/rooms/{id}", room.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").doesNotExist());
  }

  @Test
  void cancellationReleasesDatesPreservesHistoryAndRefundsOriginalAmountOnce() throws Exception {
    var room = room();
    var r = reservation(room, 5, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    room.setPrice(new BigDecimal("900000"));
    rooms.saveAndFlush(room);
    for (int i = 0; i < 2; i++)
      mvc.perform(delete("/api/reservations/{id}", r.getId()).header("Authorization", guest))
          .andExpect(status().isNoContent());
    assertThat(reservations.findById(r.getId()).orElseThrow().getStatus())
        .isEqualTo(ReservationStatus.CANCELLED);
    assertThat(detail(room, 5).path("available").asBoolean()).isTrue();
    assertThat(refunds(r.getId())).isEqualTo(1);
    assertThat(
            finance.findAll().stream()
                .filter(e -> e.getConcept().startsWith("Reembolso reserva #" + r.getId() + " ·"))
                .findFirst()
                .orElseThrow()
                .getAmount())
        .isEqualByComparingTo("300000");
    mvc.perform(get("/api/reservations/mine").header("Authorization", guest))
        .andExpect(jsonPath("$[?(@.id == " + r.getId() + ")].cancellable").value(false));
    mvc.perform(
            post("/api/reservations")
                .header("Authorization", guest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "roomId",
                            room.getId(),
                            "checkIn",
                            r.getCheckIn().toString(),
                            "checkOut",
                            r.getCheckOut().toString(),
                            "guests",
                            1))))
        .andExpect(status().isOk());
  }

  @Test
  void cancelTodayBeforeOnePmReleasesItsRoom() throws Exception {
    var room = room();
    var r = reservation(room, 0, ReservationStatus.CONFIRMED);
    room.setStatus(RoomStatus.OCCUPIED);
    rooms.saveAndFlush(room);
    mvc.perform(
            delete("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isNoContent());
    assertThat(rooms.findById(room.getId()).orElseThrow().getStatus())
        .isEqualTo(RoomStatus.AVAILABLE);
  }

  @ParameterizedTest
  @EnumSource(
      value = RoomStatus.class,
      names = {"OCCUPIED", "MAINTENANCE", "OUT_OF_SERVICE", "RESERVED"})
  void cancellingFutureStayNeverClearsCurrentOccupancyOrManualBlocks(RoomStatus state)
      throws Exception {
    var room = room();
    var future = reservation(room, 5, ReservationStatus.CONFIRMED);
    reservation(room, 0, ReservationStatus.CHECKED_IN);
    room.setStatus(state);
    rooms.saveAndFlush(room);
    mvc.perform(
            delete("/api/reservations/{id}", future.getId())
                .header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isNoContent());
    assertThat(rooms.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(state);
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"CHECKED_IN", "COMPLETED"})
  void startedAndCompletedStaysCannotBeCancelledEvenByAdmin(ReservationStatus state)
      throws Exception {
    var r = reservation(room(), 5, state);
    for (var email : List.of("huesped@lindomar.co", "admin@lindomar.co")) {
      mvc.perform(delete("/api/reservations/{id}", r.getId()).header("Authorization", auth(email)))
          .andExpect(status().isConflict());
    }
    assertThat(refunds(r.getId())).isZero();
    assertThat(reservations.findById(r.getId()).orElseThrow().getStatus()).isEqualTo(state);
  }

  @Test
  void cancellationRequiresOwnershipRoleAndValidSession() throws Exception {
    var r = reservation(room(), 5, ReservationStatus.CONFIRMED);
    mvc.perform(delete("/api/reservations/{id}", r.getId())).andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/reservations/{id}", r.getId()).header("Authorization", "Bearer fake"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            delete("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isForbidden());
    r.setGuestId(users.findByEmailIgnoreCase("ana@lindomar.co").orElseThrow().getId());
    reservations.saveAndFlush(r);
    mvc.perform(
            delete("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("ana@lindomar.co")))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/reservations/{id}", Long.MAX_VALUE)
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isNotFound());
    assertThat(refunds(r.getId())).isZero();
  }

  @Test
  void adminUpdateUsesSamePolicyAndCannotResurrectOrMoveAnExpiredReservation() throws Exception {
    var admin = auth("admin@lindomar.co");
    var r = reservation(room(), 5, ReservationStatus.CONFIRMED);
    mvc.perform(
            put("/api/reservations/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(refunds(r.getId())).isEqualTo(1);
    mvc.perform(
            put("/api/reservations/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isConflict());
    var expired = reservation(room(), -1, ReservationStatus.CONFIRMED);
    mvc.perform(
            put("/api/reservations/{id}", expired.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checkIn\":\"2030-07-10\",\"status\":\"CANCELLED\"}"))
        .andExpect(status().isConflict());
    assertThat(refunds(expired.getId())).isZero();
  }

  @Test
  void reservationCreationCannotOverwriteAnExistingId() throws Exception {
    var room = room();
    var original = reservation(room, 5, ReservationStatus.CANCELLED);
    var created =
        json.readTree(
            mvc.perform(
                    post("/api/reservations")
                        .header("Authorization", auth("huesped@lindomar.co"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "id",
                                    original.getId(),
                                    "roomId",
                                    room.getId(),
                                    "checkIn",
                                    "2030-06-15",
                                    "checkOut",
                                    "2030-06-17",
                                    "guests",
                                    1))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(created.path("id").asLong()).isNotEqualTo(original.getId());
    assertThat(reservations.findById(original.getId()).orElseThrow().getStatus())
        .isEqualTo(ReservationStatus.CANCELLED);
  }

  @Test
  void completingOnCheckoutDayReleasesRoomWithoutRefund() throws Exception {
    var room = room();
    var r = reservation(room, -2, ReservationStatus.CHECKED_IN);
    room.setStatus(RoomStatus.OCCUPIED);
    rooms.saveAndFlush(room);
    mvc.perform(
            put("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cancellable").value(false));
    assertThat(rooms.findById(room.getId()).orElseThrow().getStatus())
        .isEqualTo(RoomStatus.AVAILABLE);
    assertThat(refunds(r.getId())).isZero();
  }

  @Test
  void reservationAutomaticallyAssignsPreparationTaskToLeastBusyEmployee() throws Exception {
    var available = users.findByEmailIgnoreCase("ana@lindomar.co").orElseThrow();
    var room = room();
    var created =
        json.readTree(
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
                                    1))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    var automatic =
        tasks.findAll().stream()
            .filter(
                t -> ("RESERVATION_CREATED:" + created.path("id").asLong()).equals(t.getEventKey()))
            .toList();
    assertThat(automatic)
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.getEmployeeId()).isEqualTo(available.getId());
              assertThat(task.getTitle()).isEqualTo("Preparar habitación " + room.getNumber());
              assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2030, 6, 15));
              assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
            });
    long before = tasks.count();
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
                            "2030-06-16",
                            "checkOut",
                            "2030-06-18",
                            "guests",
                            1))))
        .andExpect(status().isConflict());
    assertThat(tasks.count()).isEqualTo(before);
  }

  @Test
  void vacatingRoomAutomaticallyAssignsOneHousekeepingTask() throws Exception {
    var room = room();
    var r = reservation(room, -2, ReservationStatus.CHECKED_IN);
    room.setStatus(RoomStatus.OCCUPIED);
    rooms.saveAndFlush(room);
    var admin = auth("admin@lindomar.co");
    mvc.perform(
            put("/api/reservations/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk());
    assertThat(
            tasks.findAll().stream()
                .filter(t -> ("ROOM_VACATED:" + r.getId()).equals(t.getEventKey()))
                .toList())
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.getTitle()).isEqualTo("Acondicionar habitación " + room.getNumber());
              assertThat(task.getDueDate()).isEqualTo(LocalDate.now(clock));
              assertThat(task.getPriority()).isEqualTo("Alta");
            });
    mvc.perform(
            put("/api/reservations/{id}", r.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isConflict());
    assertThat(
            tasks.findAll().stream()
                .filter(t -> ("ROOM_VACATED:" + r.getId()).equals(t.getEventKey()))
                .count())
        .isEqualTo(1);
  }

  @Test
  void roomDeletionCannotOrphanAReservationAndBreakItsCancellation() throws Exception {
    var room = room();
    var r = reservation(room, 5, ReservationStatus.CONFIRMED);
    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());
    mvc.perform(
            delete("/api/reservations/{id}", r.getId())
                .header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isNoContent());
    assertThat(refunds(r.getId())).isEqualTo(1);
  }

  @Test
  void adminCanAssignRolesAndAllOldSessionsAreRevoked() throws Exception {
    var admin = auth("admin@lindomar.co");
    var first = auth("huesped@lindomar.co");
    var second = auth("huesped@lindomar.co");
    var id = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow().getId();
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"EMPLOYEE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("EMPLOYEE"))
        .andExpect(jsonPath("$.password").doesNotExist());
    for (var token : List.of(first, second))
      mvc.perform(get("/api/auth/me").header("Authorization", token))
          .andExpect(status().isUnauthorized());
    var employee = auth("huesped@lindomar.co");
    mvc.perform(get("/api/tasks").header("Authorization", employee)).andExpect(status().isOk());
    mvc.perform(get("/api/reservations/mine").header("Authorization", employee))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/users").header("Authorization", employee))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/users/{id}", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/users").header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isOk());
  }

  @Test
  void roleValidationProtectsSelfAndRejectsUnknownRolesOrUsers() throws Exception {
    var admin = auth("admin@lindomar.co");
    var id = users.findByEmailIgnoreCase("admin@lindomar.co").orElseThrow().getId();
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
        .andExpect(status().isConflict());
    for (var body : List.of("{}", "{\"role\":null}", "{\"role\":\"SUPERADMIN\"}"))
      mvc.perform(
              patch("/api/users/{id}/role", id)
                  .header("Authorization", admin)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/users/{id}/role", Long.MAX_VALUE)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/roles").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ADMIN").isArray());
  }

  @Test
  void expiredSessionAndMissingRoleNeverGrantAccess() throws Exception {
    var token = auth("admin@lindomar.co");
    var session = sessions.findByToken(token.substring(7)).orElseThrow();
    session.setExpiresAt(LocalDateTime.now(clock));
    sessions.saveAndFlush(session);
    mvc.perform(get("/api/users").header("Authorization", token))
        .andExpect(status().isUnauthorized());
    var guest = auth("huesped@lindomar.co");
    var account = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow();
    account.setRole(null);
    users.saveAndFlush(account);
    mvc.perform(get("/api/tasks").header("Authorization", guest)).andExpect(status().isForbidden());
    mvc.perform(get("/api/requests").header("Authorization", guest))
        .andExpect(status().isForbidden());
  }

  @Test
  void demotionRevokesAdminAccessAndNoOpRoleChangeKeepsSessions() throws Exception {
    var admin = auth("admin@lindomar.co");
    var guest = auth("huesped@lindomar.co");
    var id = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow().getId();
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").header("Authorization", guest)).andExpect(status().isOk());
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk());
    var promoted = auth("huesped@lindomar.co");
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/users").header("Authorization", promoted))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/users").header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(strings = {"huesped@lindomar.co", "empleado@lindomar.co"})
  void restrictedRolesCannotReadOrWriteAdministrativeResources(String email) throws Exception {
    var token = auth(email);
    var id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
    for (var path :
        List.of(
            "/api/users",
            "/api/roles",
            "/api/reservations",
            "/api/finance",
            "/api/reminders",
            "/api/dashboard")) {
      mvc.perform(get(path).header("Authorization", token)).andExpect(status().isForbidden());
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    mvc.perform(
            patch("/api/users/{id}/role", id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/users/{id}", id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());
    for (var path : List.of("/api/rooms", "/api/tasks", "/api/finance", "/api/reminders"))
      mvc.perform(
              post(path)
                  .header("Authorization", token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      json.writeValueAsString(
                          Map.of(
                              "employeeId",
                              id,
                              "title",
                              "Prueba",
                              "description",
                              "Prueba de permisos",
                              "dueDate",
                              LocalDate.now().plusDays(1).toString(),
                              "priority",
                              "Alta"))))
          .andExpect(status().isForbidden());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousCancellationsCreateExactlyOneRefund() throws Exception {
    var r = reservation(room(), 5, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> cancel =
          () -> {
            start.await();
            return mvc.perform(
                    delete("/api/reservations/{id}", r.getId()).header("Authorization", guest))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var one = pool.submit(cancel);
      var two = pool.submit(cancel);
      start.countDown();
      assertThat(one.get(15, TimeUnit.SECONDS)).isEqualTo(204);
      assertThat(two.get(15, TimeUnit.SECONDS)).isEqualTo(204);
    }
    assertThat(refunds(r.getId())).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(
      value = RoomStatus.class,
      names = {"MAINTENANCE", "OUT_OF_SERVICE"})
  void operationalBlockCannotLeaveActiveReservationConfirmed(RoomStatus state) throws Exception {
    var room = room();
    var booking = reservation(room, 5, ReservationStatus.CONFIRMED);
    var admin = auth("admin@lindomar.co");
    mvc.perform(
            patch("/api/rooms/{id}/status", room.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("status", state))))
        .andExpect(status().isConflict());
    room.setStatus(state);
    mvc.perform(
            put("/api/rooms/{id}", room.getId())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(room)))
        .andExpect(status().isConflict());
    assertThat(reservations.findById(booking.getId()).orElseThrow().getStatus())
        .isEqualTo(ReservationStatus.CONFIRMED);
  }

  @Test
  void cancelledOrCompletedReservationsAllowOperationalBlock() throws Exception {
    var room = room();
    reservation(room, 5, ReservationStatus.CANCELLED);
    reservation(room, 8, ReservationStatus.COMPLETED);
    mvc.perform(
            patch("/api/rooms/{id}/status", room.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"MAINTENANCE\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void noEmployeesDoesNotPreventValidReservationAndCancellation() throws Exception {
    for (var employee : users.findByRoleOrderByIdAsc(Role.EMPLOYEE)) {
      employee.setRole(Role.GUEST);
      users.saveAndFlush(employee);
    }
    var room = room();
    var guest = auth("huesped@lindomar.co");
    var created =
        json.readTree(
            mvc.perform(
                    post("/api/reservations")
                        .header("Authorization", guest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "roomId",
                                    room.getId(),
                                    "checkIn",
                                    "2030-06-10",
                                    "checkOut",
                                    "2030-06-12",
                                    "guests",
                                    1))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    var id = created.get("id").asLong();
    mvc.perform(delete("/api/reservations/{id}", id).header("Authorization", guest))
        .andExpect(status().isNoContent());
    assertThat(refunds(id)).isEqualTo(1);
    assertThat(
            tasks.findAll().stream()
                .filter(t -> ("ROOM_VACATED:" + id).equals(t.getEventKey()))
                .findFirst()
                .orElseThrow()
                .getEmployeeId())
        .isNull();
    var pending =
        tasks.findAll().stream()
            .filter(t -> ("ROOM_VACATED:" + id).equals(t.getEventKey()))
            .findFirst()
            .orElseThrow();
    var employee = users.findByEmailIgnoreCase("empleado@lindomar.co").orElseThrow();
    employee.setRole(Role.EMPLOYEE);
    users.saveAndFlush(employee);
    mvc.perform(
            put("/api/tasks/{id}", pending.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("employeeId", employee.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeId").value(employee.getId()));
  }
}
