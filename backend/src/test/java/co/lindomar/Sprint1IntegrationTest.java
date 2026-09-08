package co.lindomar;

import co.lindomar.domain.Domain.*;
import co.lindomar.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sprint1;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class Sprint1IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoomRepository rooms;
    @Autowired ReservationRepository reservations;
    @Autowired PasswordEncoder encoder;
    @Autowired @Qualifier("migrateLegacyPasswords") CommandLineRunner migratePasswords;

    private String token(String email) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", "demo123"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private Room room() {
        return rooms.saveAndFlush(new Room("T-101", 1, "Suite", 2, new BigDecimal("150000"), true,
            true, "Wi-Fi", RoomStatus.AVAILABLE));
    }

    private Reservation reserve(Room room, ReservationStatus status) {
        var reservation = new Reservation();
        reservation.setRoomId(room.getId());
        reservation.setGuestId(users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow().getId());
        reservation.setCheckIn(LocalDate.now().plusDays(10));
        reservation.setCheckOut(LocalDate.now().plusDays(15));
        reservation.setGuests(1);
        reservation.setStatus(status);
        return reservations.saveAndFlush(reservation);
    }

    private JsonNode search(int start, int end) throws Exception {
        return json.readTree(mvc.perform(get("/api/rooms")
            .param("checkIn", LocalDate.now().plusDays(start).toString())
            .param("checkOut", LocalDate.now().plusDays(end).toString()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private boolean contains(JsonNode result, Room room) {
        for (var item : result) if (item.get("id").asLong() == room.getId()) return true;
        return false;
    }

    @Test
    void registrationHashesCredentialsNormalizesEmailAndKeepsGuestRole() throws Exception {
        var payload = Map.of("name", "  Prueba Registro  ", "email", "  NUEVO@example.com  ",
            "password", "clave123", "role", "ADMIN");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("GUEST"))
            .andExpect(jsonPath("$.user.name").value("Prueba Registro"))
            .andExpect(jsonPath("$.user.password").doesNotExist()).andExpect(jsonPath("$.token").isNotEmpty());
        var user = users.findByEmailIgnoreCase("nuevo@example.com").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo("clave123");
        assertThat(encoder.matches("clave123", user.getPassword())).isTrue();
        assertThat(json.writeValueAsString(user)).doesNotContain(user.getPassword());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"nuevo@example.com\",\"password\":\"clave123\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"nuevo@example.com\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
            .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"\",\"email\":\"a@b.com\",\"password\":\"clave123\"}",
        "{\"name\":\"Test\",\"email\":\"not-an-email@\",\"password\":\"clave123\"}",
        "{\"name\":\"Test\",\"email\":\"a@b.com\",\"password\":\"12345\"}",
        "{\"name\":\"Test\",\"email\":\"a@b.com\",\"password\":\"      \"}"})
    void registrationRejectsInvalidFields(String body) throws Exception {
        long count = users.count();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void bcryptRejectsOversizedUtf8PasswordsAndSeedPasswordsAreHashed() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "Test", "email", "a@b.com", "password", "ñ".repeat(37)))))
            .andExpect(status().isBadRequest());
        for (var user : users.findAll()) assertThat(encoder.matches("demo123", user.getPassword())).isTrue();
    }

    @Test
    void legacyPasswordMigrationIsIdempotent() throws Exception {
        var user = users.saveAndFlush(new UserAccount("Legacy", "legacy@example.com", "clave123", Role.GUEST, ""));
        migratePasswords.run();
        var hash = users.findById(user.getId()).orElseThrow().getPassword();
        assertThat(encoder.matches("clave123", hash)).isTrue();
        migratePasswords.run();
        assertThat(users.findById(user.getId()).orElseThrow().getPassword()).isEqualTo(hash);
    }

    @ParameterizedTest
    @CsvSource({"-1,1", "2,2", "3,2"})
    void searchRejectsPastEqualAndReversedDates(int start, int end) throws Exception {
        mvc.perform(get("/api/rooms").param("checkIn", LocalDate.now().plusDays(start).toString())
            .param("checkOut", LocalDate.now().plusDays(end).toString()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkIn", "checkOut"})
    void searchRejectsOneMissingDate(String field) throws Exception {
        mvc.perform(get("/api/rooms").param(field, LocalDate.now().plusDays(1).toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchRejectsMalformedDatesButPreservesUndatedCatalog() throws Exception {
        mvc.perform(get("/api/rooms").param("checkIn", "2026-02-30").param("checkOut", "invalid"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms")).andExpect(status().isOk());
        var room = room();
        assertThat(contains(search(0, 1), room)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"8,11,false", "14,17,false", "11,14,false", "9,16,false", "10,15,false",
        "8,10,true", "15,17,true", "0,1,true", "20,22,true"})
    void searchHandlesOverlapsAndAllowsAdjacentStays(int start, int end, boolean available) throws Exception {
        var room = room();
        reserve(room, ReservationStatus.CONFIRMED);
        assertThat(contains(search(start, end), room)).isEqualTo(available);
    }

    @Test
    void cancelledReservationsDoNotBlockSearchAndFiltersStillCombine() throws Exception {
        var room = room();reserve(room, ReservationStatus.CANCELLED);
        assertThat(contains(search(10, 15), room)).isTrue();
        mvc.perform(get("/api/rooms").param("type", "Suite").param("capacity", "2")
            .param("maxPrice", "150000").param("balcony", "true").param("petFriendly", "true")
            .param("checkIn", LocalDate.now().plusDays(10).toString()).param("checkOut", LocalDate.now().plusDays(15).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(room.getId()));
    }

    @Test
    void roomSearchReturnsAllDetailsNeededBeforeBooking() throws Exception {
        var room = room();
        var result = search(20, 22);
        JsonNode detail = null;
        for (var item : result) if (item.get("id").asLong() == room.getId()) detail = item;
        assertThat(detail).isNotNull();
        assertThat(detail.get("number").asText()).isEqualTo("T-101");
        assertThat(detail.get("type").asText()).isEqualTo("Suite");
        assertThat(detail.get("capacity").asInt()).isEqualTo(2);
        assertThat(detail.get("price").decimalValue()).isEqualByComparingTo("150000");
        assertThat(detail.get("balcony").asBoolean()).isTrue();
        assertThat(detail.get("petFriendly").asBoolean()).isTrue();
        assertThat(detail.get("services").asText()).isEqualTo("Wi-Fi");
        assertThat(detail.get("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void adminCanCreateAValidatedRoomAndSeeItInInventory() throws Exception {
        var admin = "Bearer " + token("admin@lindomar.co");
        var payload = Map.of("number", "  T-NEW  ", "floor", 4, "type", " Deluxe ", "capacity", 3,
            "price", 275000, "balcony", true, "petFriendly", false, "services", " Wi-Fi · TV ",
            "status", "AVAILABLE");
        var created = json.readTree(mvc.perform(post("/api/rooms").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.number").value("T-NEW"))
            .andExpect(jsonPath("$.type").value("Deluxe"))
            .andExpect(jsonPath("$.services").value("Wi-Fi · TV"))
            .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/rooms/all")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + created.get("id").asLong() + ")].number").value("T-NEW"));
        mvc.perform(post("/api/rooms").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"number\":\"\",\"floor\":0,\"type\":\"\",\"capacity\":0,\"price\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void adminCanEditPermittedRoomInformationAndInventoryReflectsIt() throws Exception {
        var room = room();
        var admin = "Bearer " + token("admin@lindomar.co");
        var payload = Map.of("number", "T-EDITED", "floor", 5, "type", "Familiar", "capacity", 4,
            "price", 325000, "balcony", false, "petFriendly", true, "services", "Wi-Fi · Cocina",
            "status", "MAINTENANCE");
        mvc.perform(put("/api/rooms/{id}", room.getId()).header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(room.getId()))
            .andExpect(jsonPath("$.number").value("T-EDITED")).andExpect(jsonPath("$.floor").value(5))
            .andExpect(jsonPath("$.capacity").value(4)).andExpect(jsonPath("$.status").value("MAINTENANCE"));
        var stored = rooms.findById(room.getId()).orElseThrow();
        assertThat(stored.getNumber()).isEqualTo("T-EDITED");
        assertThat(stored.getType()).isEqualTo("Familiar");
        assertThat(stored.getServices()).isEqualTo("Wi-Fi · Cocina");
        assertThat(stored.isPetFriendly()).isTrue();
    }

    @Test
    void adminReservationListRequiresAdminAndContainsCompleteReservationData() throws Exception {
        var room = room();
        var reservation = reserve(room, ReservationStatus.CONFIRMED);
        mvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + token("huesped@lindomar.co")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + token("empleado@lindomar.co")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + token("admin@lindomar.co")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + reservation.getId() + ")].guestName").value("Mariana Torres"))
            .andExpect(jsonPath("$[?(@.id == " + reservation.getId() + ")].roomNumber").value("T-101"))
            .andExpect(jsonPath("$[?(@.id == " + reservation.getId() + ")].status").value("CONFIRMED"));
    }

    @Test
    void guestCannotReadAdministratorEndpoints() throws Exception {
        var auth = "Bearer " + token("huesped@lindomar.co");
        for (var path : new String[]{"/api/users", "/api/reservations", "/api/finance", "/api/reminders", "/api/dashboard", "/api/tasks"}) {
            mvc.perform(get(path).header("Authorization", auth)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/requests").header("Authorization", auth)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(RoomStatus.class)
    void adminCanPersistEveryStatusAndUnavailableRoomsCannotBeBooked(RoomStatus state) throws Exception {
        var room = room();
        var admin = token("admin@lindomar.co");
        mvc.perform(patch("/api/rooms/{id}/status", room.getId()).header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status", state))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state.name()))
            .andExpect(jsonPath("$.number").value(room.getNumber()));
        assertThat(rooms.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(state);
        assertThat(contains(search(20, 22), room)).isEqualTo(state == RoomStatus.AVAILABLE);
        if (state != RoomStatus.AVAILABLE) {
            mvc.perform(post("/api/reservations").header("Authorization", "Bearer " + token("huesped@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("roomId", room.getId(),
                    "checkIn", LocalDate.now().plusDays(20).toString(), "checkOut", LocalDate.now().plusDays(22).toString(), "guests", 1))))
                .andExpect(status().isConflict());
        }
        mvc.perform(get("/api/rooms/all")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + room.getId() + ")].status").value(state.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"huesped@lindomar.co", "empleado@lindomar.co"})
    void nonAdminsCannotChangeStatusOrPromoteThemselves(String email) throws Exception {
        var room = room();var auth = "Bearer " + token(email);
        mvc.perform(patch("/api/rooms/{id}/status", room.getId()).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESERVED\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/rooms/{id}", room.getId()).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(room)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/rooms").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(room)))
            .andExpect(status().isForbidden());
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        mvc.perform(put("/api/users/{id}", user.getId()).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
        assertThat(rooms.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void statusRequiresAuthenticationAndValidExistingRoom() throws Exception {
        var room = room();
        mvc.perform(patch("/api/rooms/{id}/status", room.getId()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"RESERVED\"}")).andExpect(status().isUnauthorized());
        var auth = "Bearer " + token("admin@lindomar.co");
        for (var body : new String[]{"{}", "{\"status\":null}", "{\"status\":\"INVALID\"}"}) {
            mvc.perform(patch("/api/rooms/{id}/status", room.getId()).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/rooms/{id}/status", Long.MAX_VALUE).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESERVED\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/rooms/{id}", room.getId()).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESERVED\"}"))
            .andExpect(status().isBadRequest());
        room.setStatus(RoomStatus.OUT_OF_SERVICE);
        mvc.perform(put("/api/rooms/{id}", room.getId()).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(room)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    }
}
