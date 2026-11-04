package co.lindomar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.Room;
import co.lindomar.domain.Domain.RoomStatus;
import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.ReservationRepository;
import co.lindomar.repository.RoomRepository;
import co.lindomar.repository.TaskRepository;
import co.lindomar.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas automatizadas del Sprint 3 para: - HU-19: Delete Room (eliminación segura de
 * habitaciones, conflictos y permisos). - HU-23: Manage User Accounts (creación, edición,
 * eliminación y validaciones de cuentas de usuario).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:sprint3_users_rooms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class UserAndRoomStoriesSprint3Test {

  @TestConfiguration
  static class FixedTimeConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2030-10-15T10:00:00Z"), ZoneId.of("America/Bogota"));
    }
  }

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private RoomRepository rooms;
  @Autowired private UserRepository users;
  @Autowired private ReservationRepository reservations;
  @Autowired private TaskRepository tasks;
  @Autowired private PasswordEncoder passwords;

  private String auth(String email) throws Exception {
    var res =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(Map.of("email", email, "password", "demo123"))))
            .andExpect(status().isOk())
            .andReturn();
    return "Bearer " + json.readTree(res.getResponse().getContentAsString()).get("token").asText();
  }

  private Room createRoom(String number, RoomStatus status) {
    var room = new Room();
    room.setNumber(number);
    room.setFloor(1);
    room.setType("Estándar");
    room.setCapacity(2);
    room.setPrice(new BigDecimal("180000"));
    room.setBalcony(false);
    room.setPetFriendly(false);
    room.setServices("Wi-Fi");
    room.setImageUrl("");
    room.setStatus(status);
    return rooms.save(room);
  }

  // ==========================================
  // HU-19: Delete Room
  // ==========================================

  @Test
  void adminCanDeleteRoomWithoutReservations() throws Exception {
    var room = createRoom("901", RoomStatus.AVAILABLE);

    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isNoContent());

    assertThat(rooms.findById(room.getId())).isEmpty();
  }

  @Test
  void deleteRoomRejectsOccupiedRoom() throws Exception {
    var room = createRoom("902", RoomStatus.OCCUPIED);

    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());

    assertThat(rooms.findById(room.getId())).isPresent();
  }

  @Test
  void deleteRoomRejectsRoomWithReservations() throws Exception {
    var room = createRoom("903", RoomStatus.AVAILABLE);
    var guest = users.findByEmailIgnoreCase("guest@lindomar.co").orElseThrow();

    var res = new Reservation();
    res.setRoomId(room.getId());
    res.setGuestId(guest.getId());
    res.setCheckIn(LocalDate.of(2030, 10, 20));
    res.setCheckOut(LocalDate.of(2030, 10, 22));
    res.setGuests(2);
    res.setStatus(ReservationStatus.CONFIRMED);
    res.setAmount(new BigDecimal("360000"));
    reservations.save(res);

    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());

    assertThat(rooms.findById(room.getId())).isPresent();
  }

  @Test
  void nonAdminCannotDeleteRoom() throws Exception {
    var room = createRoom("904", RoomStatus.AVAILABLE);

    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("guest@lindomar.co")))
        .andExpect(status().isForbidden());

    mvc.perform(
            delete("/api/rooms/{id}", room.getId())
                .header("Authorization", auth("employee@lindomar.co")))
        .andExpect(status().isForbidden());

    mvc.perform(delete("/api/rooms/{id}", room.getId())).andExpect(status().isUnauthorized());
  }

  @Test
  void deleteRoomReturnsNotFoundForNonExistentRoom() throws Exception {
    mvc.perform(
            delete("/api/rooms/{id}", 999999L).header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isNotFound());
  }

  // ==========================================
  // HU-23: Manage User Accounts
  // ==========================================

  @Test
  void adminCanCreateNewEmployeeOrGuestAccount() throws Exception {
    var newUserData =
        Map.of(
            "name", "Carlos Mesero",
            "email", "carlos.empleado@lindomar.co",
            "phone", "3001234567",
            "role", "EMPLOYEE",
            "password", "claveSegura123");

    mvc.perform(
            post("/api/users")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(newUserData)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Carlos Mesero"))
        .andExpect(jsonPath("$.email").value("carlos.empleado@lindomar.co"))
        .andExpect(jsonPath("$.role").value("EMPLOYEE"))
        .andExpect(jsonPath("$.phone").value("3001234567"));

    var saved = users.findByEmailIgnoreCase("carlos.empleado@lindomar.co").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(Role.EMPLOYEE);
    assertThat(passwords.matches("claveSegura123", saved.getPassword())).isTrue();

    // El nuevo usuario puede iniciar sesión de inmediato
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "email", "carlos.empleado@lindomar.co",
                            "password", "claveSegura123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("EMPLOYEE"));
  }

  @Test
  void adminCreateUserRejectsDuplicateEmail() throws Exception {
    var duplicateData =
        Map.of(
            "name", "Otro Admin",
            "email", "admin@lindomar.co", // ya existe en SeedData
            "phone", "3119876543",
            "role", "ADMIN",
            "password", "password123");

    mvc.perform(
            post("/api/users")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(duplicateData)))
        .andExpect(status().isConflict());
  }

  @Test
  void adminCreateUserValidatesFields() throws Exception {
    var invalidData =
        Map.of(
            "name", "",
            "email", "correo-invalido",
            "role", "GUEST",
            "password", "123"); // menor a 6 caracteres

    mvc.perform(
            post("/api/users")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalidData)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminCanUpdateUserDetailsAndRole() throws Exception {
    var user = new UserAccount();
    user.setName("Temporal Para Edicion");
    user.setEmail("temporal.edit@lindomar.co");
    user.setPhone("3000000000");
    user.setRole(Role.GUEST);
    user.setPassword(passwords.encode("demo123"));
    user = users.save(user);

    var updateData =
        Map.of(
            "name", "Nombre Actualizado",
            "phone", "3999999999",
            "role", "EMPLOYEE");

    mvc.perform(
            put("/api/users/{id}", user.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(updateData)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nombre Actualizado"))
        .andExpect(jsonPath("$.phone").value("3999999999"))
        .andExpect(jsonPath("$.role").value("EMPLOYEE"));

    var refreshed = users.findById(user.getId()).orElseThrow();
    assertThat(refreshed.getName()).isEqualTo("Nombre Actualizado");
    assertThat(refreshed.getPhone()).isEqualTo("3999999999");
    assertThat(refreshed.getRole()).isEqualTo(Role.EMPLOYEE);
  }

  @Test
  void adminCanDeleteUserWithoutObligations() throws Exception {
    var user = new UserAccount();
    user.setName("Usuario Libre");
    user.setEmail("usuario.libre@lindomar.co");
    user.setPhone("3111111111");
    user.setRole(Role.GUEST);
    user.setPassword(passwords.encode("demo123"));
    user = users.save(user);

    mvc.perform(
            delete("/api/users/{id}", user.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isNoContent());

    assertThat(users.findById(user.getId())).isEmpty();
  }

  @Test
  void adminCannotDeleteSelf() throws Exception {
    var currentAdmin = users.findByEmailIgnoreCase("admin@lindomar.co").orElseThrow();

    mvc.perform(
            delete("/api/users/{id}", currentAdmin.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());

    assertThat(users.findById(currentAdmin.getId())).isPresent();
  }

  @Test
  void adminCannotDeleteUserWithReservations() throws Exception {
    var guest = users.findByEmailIgnoreCase("guest@lindomar.co").orElseThrow();
    var room = rooms.findAll().getFirst();

    var res = new Reservation();
    res.setRoomId(room.getId());
    res.setGuestId(guest.getId());
    res.setCheckIn(LocalDate.of(2030, 10, 20));
    res.setCheckOut(LocalDate.of(2030, 10, 22));
    res.setGuests(2);
    res.setStatus(ReservationStatus.CONFIRMED);
    res.setAmount(new BigDecimal("300000"));
    reservations.save(res);

    mvc.perform(
            delete("/api/users/{id}", guest.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());

    assertThat(users.findById(guest.getId())).isPresent();
  }

  @Test
  void adminCannotDeleteEmployeeWithPendingTasks() throws Exception {
    var employee = users.findByEmailIgnoreCase("employee@lindomar.co").orElseThrow();

    var task =
        new StaffTask(
            employee.getId(),
            "Aseo de habitación",
            "Limpieza de piso 2",
            TaskStatus.PENDING,
            LocalDate.of(2030, 10, 16),
            "Alta");
    tasks.save(task);

    mvc.perform(
            delete("/api/users/{id}", employee.getId())
                .header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isConflict());

    assertThat(users.findById(employee.getId())).isPresent();
  }

  @Test
  void nonAdminCannotManageUsers() throws Exception {
    var newUserData =
        Map.of(
            "name", "Intruso",
            "email", "intruso@lindomar.co",
            "role", "ADMIN",
            "password", "clave123");

    mvc.perform(
            post("/api/users")
                .header("Authorization", auth("guest@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(newUserData)))
        .andExpect(status().isForbidden());

    mvc.perform(delete("/api/users/{id}", 1L).header("Authorization", auth("employee@lindomar.co")))
        .andExpect(status().isForbidden());
  }

  // ==========================================
  // HU-21: Modify Guest Reservation (Admin)
  // ==========================================

  @Test
  void adminCanModifyReservationRoomDatesAndGuests() throws Exception {
    var room1 = createRoom("701", RoomStatus.AVAILABLE);
    var room2 = createRoom("702", RoomStatus.AVAILABLE);
    var guest = users.findByEmailIgnoreCase("guest@lindomar.co").orElseThrow();

    var res = new Reservation();
    res.setRoomId(room1.getId());
    res.setGuestId(guest.getId());
    res.setCheckIn(LocalDate.of(2030, 10, 20));
    res.setCheckOut(LocalDate.of(2030, 10, 22));
    res.setGuests(1);
    res.setStatus(ReservationStatus.CONFIRMED);
    res.setAmount(new BigDecimal("360000"));
    res = reservations.save(res);

    var modification = new Reservation();
    modification.setRoomId(room2.getId());
    modification.setCheckIn(LocalDate.of(2030, 10, 21));
    modification.setCheckOut(LocalDate.of(2030, 10, 24));
    modification.setGuests(2);
    modification.setNotes("Cambio por solicitud de habitación con vista");

    mvc.perform(
            put("/api/reservations/{id}", res.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(modification)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(room2.getId()))
        .andExpect(jsonPath("$.checkIn").value("2030-10-21"))
        .andExpect(jsonPath("$.checkOut").value("2030-10-24"))
        .andExpect(jsonPath("$.guests").value(2))
        .andExpect(jsonPath("$.notes").value("Cambio por solicitud de habitación con vista"));

    var updated = reservations.findById(res.getId()).orElseThrow();
    assertThat(updated.getRoomId()).isEqualTo(room2.getId());
    assertThat(updated.getCheckIn()).isEqualTo(LocalDate.of(2030, 10, 21));
    assertThat(updated.getCheckOut()).isEqualTo(LocalDate.of(2030, 10, 24));
    assertThat(updated.getGuests()).isEqualTo(2);
    // 3 noches * 180.000 = 540.000
    assertThat(updated.getAmount()).isEqualByComparingTo("540000");
  }

  @Test
  void adminModifyReservationRejectsOccupiedRoomConflict() throws Exception {
    var room = createRoom("703", RoomStatus.AVAILABLE);
    var guest = users.findByEmailIgnoreCase("guest@lindomar.co").orElseThrow();

    // Reserva 1 existente: 20 a 25 oct
    var existing = new Reservation();
    existing.setRoomId(room.getId());
    existing.setGuestId(guest.getId());
    existing.setCheckIn(LocalDate.of(2030, 10, 20));
    existing.setCheckOut(LocalDate.of(2030, 10, 25));
    existing.setGuests(2);
    existing.setStatus(ReservationStatus.CONFIRMED);
    existing.setAmount(new BigDecimal("900000"));
    reservations.save(existing);

    // Reserva 2 en otra fecha: 27 a 29 oct
    var target = new Reservation();
    target.setRoomId(createRoom("704", RoomStatus.AVAILABLE).getId());
    target.setGuestId(guest.getId());
    target.setCheckIn(LocalDate.of(2030, 10, 27));
    target.setCheckOut(LocalDate.of(2030, 10, 29));
    target.setGuests(1);
    target.setStatus(ReservationStatus.CONFIRMED);
    target.setAmount(new BigDecimal("360000"));
    target = reservations.save(target);

    // Intentar mover target a room en fecha que colisiona (22 a 26)
    var change = new Reservation();
    change.setRoomId(room.getId());
    change.setCheckIn(LocalDate.of(2030, 10, 22));
    change.setCheckOut(LocalDate.of(2030, 10, 26));

    mvc.perform(
            put("/api/reservations/{id}", target.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(change)))
        .andExpect(status().isConflict());
  }

  // ==========================================
  // HU-03: Manage Personal Information (Guest)
  // ==========================================

  @Test
  void guestCanViewOwnProfile() throws Exception {
    mvc.perform(get("/api/auth/me").header("Authorization", auth("guest@lindomar.co")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("guest@lindomar.co"))
        .andExpect(jsonPath("$.role").value("GUEST"))
        .andExpect(jsonPath("$.name").isString());
  }

  @Test
  void guestCanUpdateAllowedPersonalFields() throws Exception {
    var update =
        Map.of(
            "name", "Laura Huesped Actualizada",
            "phone", "3209876543");

    mvc.perform(
            put("/api/auth/profile")
                .header("Authorization", auth("guest@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Laura Huesped Actualizada"))
        .andExpect(jsonPath("$.phone").value("3209876543"))
        .andExpect(jsonPath("$.email").value("guest@lindomar.co"))
        .andExpect(jsonPath("$.role").value("GUEST"));

    var guest = users.findByEmailIgnoreCase("guest@lindomar.co").orElseThrow();
    assertThat(guest.getName()).isEqualTo("Laura Huesped Actualizada");
    assertThat(guest.getPhone()).isEqualTo("3209876543");
  }

  @Test
  void guestCanUpdatePasswordAndLoginWithNewOne() throws Exception {
    var update =
        Map.of(
            "name", "Usuario Con Nueva Clave",
            "password", "nuevaClaveSuperSegura123");

    mvc.perform(
            put("/api/auth/profile")
                .header("Authorization", auth("guest@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(update)))
        .andExpect(status().isOk());

    // Iniciar sesión con la nueva contraseña
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "email", "guest@lindomar.co",
                            "password", "nuevaClaveSuperSegura123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.name").value("Usuario Con Nueva Clave"));
  }

  @Test
  void guestUpdateProfileValidatesInput() throws Exception {
    var invalidName = Map.of("name", "");

    mvc.perform(
            put("/api/auth/profile")
                .header("Authorization", auth("guest@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalidName)))
        .andExpect(status().isBadRequest());

    var shortPassword = Map.of("name", "Nombre Valido", "password", "123");

    mvc.perform(
            put("/api/auth/profile")
                .header("Authorization", auth("guest@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(shortPassword)))
        .andExpect(status().isBadRequest());
  }
}
