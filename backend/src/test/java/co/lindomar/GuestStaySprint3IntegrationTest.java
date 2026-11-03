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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** HU-10 y HU-12: reglas de negocio, integración de roles y concurrencia. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:gueststay3;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class GuestStaySprint3IntegrationTest {
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
  @Autowired ReservationRepository reservations;
  @Autowired RoomRepository rooms;
  @Autowired FinanceRepository finance;
  @Autowired TaskRepository tasks;
  @Autowired UserRepository users;
  @Autowired RequestRepository requests;
  @Autowired Clock clock;

  private String auth(String email) throws Exception {
    return "Bearer "
        + read(mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(Map.of("email", email, "password", "demo123")))))
            .path("token")
            .asText();
  }

  private JsonNode read(ResultActions result) throws Exception {
    return json.readTree(
        result
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  private Room room(String price) {
    return rooms.saveAndFlush(
        new Room(
            "HU3-" + UUID.randomUUID(),
            3,
            "Suite",
            3,
            new BigDecimal(price),
            true,
            false,
            "Wi-Fi",
            RoomStatus.AVAILABLE));
  }

  private Reservation booking(Room room, int days, ReservationStatus status) {
    var r = new Reservation();
    r.setGuestId(users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow().getId());
    r.setRoomId(room.getId());
    r.setCheckIn(LocalDate.now(clock).plusDays(days));
    r.setCheckOut(r.getCheckIn().plusDays(2));
    r.setGuests(2);
    r.setStatus(status);
    r.setAmount(room.getPrice().multiply(BigDecimal.valueOf(2)));
    return reservations.saveAndFlush(r);
  }

  private Map<String, Object> change(Reservation r, Room target, int days, int nights) {
    return new HashMap<>(
        Map.of(
            "roomId",
            target.getId(),
            "checkIn",
            LocalDate.now(clock).plusDays(days).toString(),
            "checkOut",
            LocalDate.now(clock).plusDays(days + nights).toString(),
            "guests",
            2,
            "notes",
            "  Cambio de estadía  ",
            "expectedVersion",
            r.getVersion(),
            "expectedTotal",
            target.getPrice().multiply(BigDecimal.valueOf(nights))));
  }

  private ResultActions modify(Long id, String token, Map<String, Object> data) throws Exception {
    return mvc.perform(
        patch("/api/reservations/{id}", id)
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(data)));
  }

  private Map<String, Object> assistance(Reservation r, UUID key) {
    return new HashMap<>(
        Map.of(
            "reservationId",
            r.getId(),
            "category",
            "Toallas y lencería",
            "notes",
            "  Dos toallas adicionales  ",
            "clientRequestId",
            key));
  }

  private ResultActions request(String token, Map<String, Object> data) throws Exception {
    return mvc.perform(
        post("/api/requests")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(data)));
  }

  @Test
  void quoteHasNoSideEffectsAndChangeAdjustsIncomeAndPreparation() throws Exception {
    var guest = auth("huesped@lindomar.co");
    var first = room("100000");
    var target = room("150000");
    var created =
        read(
            mvc.perform(
                post("/api/reservations")
                    .header("Authorization", guest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "roomId",
                                first.getId(),
                                "checkIn",
                                "2030-06-15",
                                "checkOut",
                                "2030-06-17",
                                "guests",
                                2)))));
    var r = reservations.findById(created.path("id").asLong()).orElseThrow();
    long oldVersion = r.getVersion();
    var data = change(r, target, 7, 3);
    long beforeFinance = finance.count();
    long beforeTasks = tasks.count();
    var quote =
        read(
            mvc.perform(
                post("/api/reservations/{id}/quote", r.getId())
                    .header("Authorization", guest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(data))));
    assertThat(quote.path("total").decimalValue()).isEqualByComparingTo("450000");
    assertThat(quote.path("difference").decimalValue()).isEqualByComparingTo("250000");
    assertThat(finance.count()).isEqualTo(beforeFinance);
    assertThat(tasks.count()).isEqualTo(beforeTasks);
    assertThat(r.getRoomId()).isEqualTo(first.getId());
    var saved = read(modify(r.getId(), guest, data));
    assertThat(saved.path("roomId").asLong()).isEqualTo(target.getId());
    assertThat(saved.path("version").asLong()).isGreaterThan(oldVersion);
    assertThat(saved.path("notes").asText()).isEqualTo("Cambio de estadía");
    assertThat(finance.count()).isEqualTo(beforeFinance + 1);
    assertThat(
            finance.findAll().stream()
                .filter(e -> e.getConcept().startsWith("Modificación reserva #" + r.getId())))
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.getType()).isEqualTo(EntryType.INCOME);
              assertThat(e.getAmount()).isEqualByComparingTo("250000");
            });
    assertThat(tasks.count()).isEqualTo(beforeTasks);
    var task = tasks.findByEventKey("RESERVATION_CREATED:" + r.getId()).orElseThrow();
    assertThat(task.getTitle()).isEqualTo("Preparar habitación " + target.getNumber());
    assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2030, 6, 17));
    // El huésped no puede repetir un formulario viejo y duplicar el ajuste.
    data.put("expectedVersion", oldVersion);
    modify(r.getId(), guest, data).andExpect(status().isConflict());
    assertThat(finance.count()).isEqualTo(beforeFinance + 1);
  }

  @Test
  void cheaperChangeThenCancellationRefundsOnlyTheRemainingTotal() throws Exception {
    var guest = auth("huesped@lindomar.co");
    var room = room("100000");
    var created =
        read(
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
                                "2030-06-15",
                                "checkOut",
                                "2030-06-17",
                                "guests",
                                2)))));
    var r = reservations.findById(created.path("id").asLong()).orElseThrow();
    read(modify(r.getId(), guest, change(r, room, 5, 1)));
    mvc.perform(delete("/api/reservations/{id}", r.getId()).header("Authorization", guest))
        .andExpect(status().isNoContent());
    var entries =
        finance.findAll().stream()
            .filter(
                e ->
                    e.getConcept().contains("reserva #" + r.getId() + " ·")
                        || e.getConcept().startsWith("Reserva #" + r.getId() + " ·"))
            .toList();
    var net =
        entries.stream()
            .map(e -> e.getType() == EntryType.INCOME ? e.getAmount() : e.getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(entries).hasSize(3);
    assertThat(net).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void notesOnlyPreserveAgreedPriceAndDoNotCreateFinancialMovement() throws Exception {
    var room = room("100000");
    var r = booking(room, 5, ReservationStatus.CONFIRMED);
    room.setPrice(new BigDecimal("200000"));
    rooms.saveAndFlush(room);
    var data = change(r, room, 5, 2);
    data.put("expectedTotal", new BigDecimal("200000"));
    data.put("guests", 3);
    long before = finance.count();
    var saved = read(modify(r.getId(), auth("huesped@lindomar.co"), data));
    assertThat(saved.path("total").decimalValue()).isEqualByComparingTo("200000");
    assertThat(saved.path("guests").asInt()).isEqualTo(3);
    assertThat(finance.count()).isEqualTo(before);
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"CANCELLED", "COMPLETED", "CHECKED_IN"})
  void nonConfirmedReservationsCannotBeModified(ReservationStatus status) throws Exception {
    var room = room("100000");
    var r = booking(room, 5, status);
    modify(r.getId(), auth("huesped@lindomar.co"), change(r, room, 6, 2))
        .andExpect(status().isConflict());
  }

  @Test
  void originalArrivalTodayCannotBeMovedToEvadeThePolicy() throws Exception {
    var room = room("100000");
    var r = booking(room, 0, ReservationStatus.CONFIRMED);
    modify(r.getId(), auth("huesped@lindomar.co"), change(r, room, 5, 2))
        .andExpect(status().isConflict());
  }

  @Test
  void modificationRequiresOwnerGuestAndLeavesStateUntouchedOnFailure() throws Exception {
    var room = room("100000");
    var r = booking(room, 5, ReservationStatus.CONFIRMED);
    var data = change(r, room, 6, 2);
    for (var email : List.of("admin@lindomar.co", "empleado@lindomar.co"))
      modify(r.getId(), auth(email), data).andExpect(status().isForbidden());
    r.setGuestId(users.findByEmailIgnoreCase("ana@lindomar.co").orElseThrow().getId());
    reservations.saveAndFlush(r);
    modify(r.getId(), auth("huesped@lindomar.co"), data).andExpect(status().isForbidden());
    assertThat(r.getCheckIn()).isEqualTo(LocalDate.of(2030, 6, 15));
    mvc.perform(
            patch("/api/reservations/{id}", r.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(data)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void invalidDatesCapacityAndMissingQuoteAreRejected() throws Exception {
    var room = room("100000");
    var r = booking(room, 5, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    for (var invalid :
        List.of(
            Map.entry("checkIn", "2030-06-10"),
            Map.entry("checkOut", "2030-06-15"),
            Map.entry("guests", 4),
            Map.entry("guests", 0),
            Map.entry("notes", "x".repeat(256)))) {
      var data = change(r, room, 5, 2);
      data.put(invalid.getKey(), invalid.getValue());
      modify(r.getId(), guest, data).andExpect(status().isBadRequest());
    }
    var data = change(r, room, 6, 2);
    data.remove("expectedTotal");
    modify(r.getId(), guest, data).andExpect(status().isBadRequest());
    data.put("expectedTotal", 1);
    modify(r.getId(), guest, data).andExpect(status().isConflict());
    data.put("roomId", Long.MAX_VALUE);
    modify(r.getId(), guest, data).andExpect(status().isNotFound());
  }

  @Test
  void overlapIsRejectedButBoundaryAndCancelledReservationsDoNotBlock() throws Exception {
    var room = room("100000");
    var r = booking(room, 5, ReservationStatus.CONFIRMED);
    booking(room, 9, ReservationStatus.CONFIRMED);
    booking(room, 7, ReservationStatus.CANCELLED);
    var guest = auth("huesped@lindomar.co");
    modify(r.getId(), guest, change(r, room, 8, 2)).andExpect(status().isConflict());
    read(modify(r.getId(), guest, change(r, room, 7, 2)));
    assertThat(r.getCheckOut()).isEqualTo(LocalDate.of(2030, 6, 19));
  }

  @ParameterizedTest
  @EnumSource(
      value = RoomStatus.class,
      names = {"MAINTENANCE", "OUT_OF_SERVICE", "RESERVED", "OCCUPIED"})
  void roomMustBeOperationallyAvailable(RoomStatus state) throws Exception {
    var first = room("100000");
    var target = room("100000");
    var r = booking(first, 5, ReservationStatus.CONFIRMED);
    target.setStatus(state);
    rooms.saveAndFlush(target);
    modify(r.getId(), auth("huesped@lindomar.co"), change(r, target, 6, 2))
        .andExpect(status().isConflict());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void twoChangesCannotClaimSameRoomAndPeriod() throws Exception {
    var r1 = booking(room("100000"), 5, ReservationStatus.CONFIRMED);
    var r2 = booking(room("100000"), 5, ReservationStatus.CONFIRMED);
    var target = room("150000");
    var guest = auth("huesped@lindomar.co");
    var data1 = change(r1, target, 6, 2);
    var data2 = change(r2, target, 6, 2);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var one =
          pool.submit(
              () -> {
                start.await();
                return modify(r1.getId(), guest, data1).andReturn().getResponse().getStatus();
              });
      var two =
          pool.submit(
              () -> {
                start.await();
                return modify(r2.getId(), guest, data2).andReturn().getResponse().getStatus();
              });
      start.countDown();
      assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            reservations.findAll().stream()
                .filter(r -> r.getRoomId().equals(target.getId()))
                .count())
        .isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"CONFIRMED", "CHECKED_IN"})
  void assistanceUsesOwnCurrentStayAndServerControlledIdentity(ReservationStatus state)
      throws Exception {
    var room = room("100000");
    var r = booking(room, 0, state);
    var data = assistance(r, UUID.randomUUID());
    data.put("roomNumber", "FORGED");
    data.put("guestId", 999999);
    data.put("status", "RESOLVED");
    data.put("id", 999999);
    data.put("createdAt", "1990-01-01T00:00:00");
    var saved = read(request(auth("huesped@lindomar.co"), data));
    assertThat(saved.path("roomNumber").asText()).isEqualTo(room.getNumber());
    assertThat(saved.path("guestId").asLong()).isEqualTo(r.getGuestId());
    assertThat(saved.path("status").asText()).isEqualTo("OPEN");
    assertThat(saved.path("createdAt").asText()).startsWith("2030-06-10T11:00");
    assertThat(saved.path("notes").asText()).isEqualTo("Dos toallas adicionales");
    assertThat(saved.path("id").asLong()).isNotEqualTo(999999);
  }

  @Test
  void eligibleStayListExcludesOtherGuestsAndInactivePeriods() throws Exception {
    var current = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    var future = booking(room("100000"), 1, ReservationStatus.CONFIRMED);
    var ended = booking(room("100000"), -2, ReservationStatus.CHECKED_IN);
    var foreign = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    foreign.setGuestId(users.findByEmailIgnoreCase("ana@lindomar.co").orElseThrow().getId());
    reservations.saveAndFlush(foreign);
    var result =
        read(
            mvc.perform(
                get("/api/requests/eligible-reservations")
                    .header("Authorization", auth("huesped@lindomar.co"))));
    var ids = new ArrayList<Long>();
    result.forEach(node -> ids.add(node.path("id").asLong()));
    assertThat(ids)
        .contains(current.getId())
        .doesNotContain(future.getId(), ended.getId(), foreign.getId());
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"CANCELLED", "COMPLETED"})
  void inactiveReservationCannotRequestAssistance(ReservationStatus state) throws Exception {
    var r = booking(room("100000"), 0, state);
    request(auth("huesped@lindomar.co"), assistance(r, UUID.randomUUID()))
        .andExpect(status().isConflict());
  }

  @Test
  void futureExpiredAndForeignStaysCannotRequestAssistance() throws Exception {
    var guest = auth("huesped@lindomar.co");
    for (int days : new int[] {-2, 1}) {
      var r = booking(room("100000"), days, ReservationStatus.CONFIRMED);
      request(guest, assistance(r, UUID.randomUUID())).andExpect(status().isConflict());
    }
    var r = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    r.setGuestId(users.findByEmailIgnoreCase("ana@lindomar.co").orElseThrow().getId());
    reservations.saveAndFlush(r);
    request(guest, assistance(r, UUID.randomUUID())).andExpect(status().isForbidden());
  }

  @Test
  void assistanceValidationAndPermissionsDoNotCreateRecords() throws Exception {
    var r = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    long before = requests.count();
    for (var invalid :
        List.of(
            Map.entry("notes", "   "),
            Map.entry("notes", "x".repeat(1201)),
            Map.entry("category", "Invalid"),
            Map.entry("clientRequestId", "invalid-uuid"))) {
      var data = assistance(r, UUID.randomUUID());
      data.put(invalid.getKey(), invalid.getValue());
      request(guest, data).andExpect(status().isBadRequest());
    }
    var data = assistance(r, UUID.randomUUID());
    data.remove("reservationId");
    request(guest, data).andExpect(status().isBadRequest());
    for (var email : List.of("admin@lindomar.co", "empleado@lindomar.co"))
      request(auth(email), assistance(r, UUID.randomUUID())).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(assistance(r, UUID.randomUUID()))))
        .andExpect(status().isUnauthorized());
    assertThat(requests.count()).isEqualTo(before);
  }

  @Test
  void retryPreservesOneRequestAndStaffLifecycleIsVisibleOnlyToOwnerAndStaff() throws Exception {
    var r = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    var data = assistance(r, UUID.randomUUID());
    long before = requests.count();
    var saved = read(request(guest, data));
    long id = saved.path("id").asLong();
    assertThat(read(request(guest, data)).path("id").asLong()).isEqualTo(id);
    assertThat(requests.count()).isEqualTo(before + 1);
    var employee = auth("empleado@lindomar.co");
    mvc.perform(
            put("/api/requests/{id}", id)
                .header("Authorization", employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}"))
        .andExpect(status().isConflict());
    for (var status : List.of("IN_PROGRESS", "RESOLVED")) {
      mvc.perform(
              put("/api/requests/{id}", id)
                  .header("Authorization", guest)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(Map.of("status", status))))
          .andExpect(status().isForbidden());
      mvc.perform(
              put("/api/requests/{id}", id)
                  .header("Authorization", employee)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(Map.of("status", status))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(status));
    }
    // Un reintento después de atenderla tampoco vuelve a abrirla ni la duplica.
    assertThat(read(request(guest, data)).path("status").asText()).isEqualTo("RESOLVED");
    mvc.perform(
            get("/api/requests")
                .header("Authorization", guest)
                .param("guestId", String.valueOf(r.getGuestId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("RESOLVED"));
    mvc.perform(get("/api/requests").header("Authorization", guest).param("guestId", "999999"))
        .andExpect(status().isForbidden());
    var altered = assistance(r, UUID.fromString(data.get("clientRequestId").toString()));
    altered.put("notes", "Otra necesidad");
    request(guest, altered).andExpect(status().isConflict());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousAssistanceRetriesCreateOnlyOneRequest() throws Exception {
    var r = booking(room("100000"), 0, ReservationStatus.CONFIRMED);
    var guest = auth("huesped@lindomar.co");
    var key = UUID.randomUUID();
    var data = assistance(r, key);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Long> action =
          () -> {
            start.await();
            return read(request(guest, data)).path("id").asLong();
          };
      var one = pool.submit(action);
      var two = pool.submit(action);
      start.countDown();
      assertThat(one.get(15, TimeUnit.SECONDS)).isEqualTo(two.get(15, TimeUnit.SECONDS));
    }
    assertThat(
            requests.findAll().stream()
                .filter(value -> key.equals(value.getClientRequestId()))
                .count())
        .isEqualTo(1);
  }
}
