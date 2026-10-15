package co.lindomar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.lindomar.domain.Domain.Room;
import co.lindomar.domain.Domain.RoomStatus;
import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.RoomRepository;
import co.lindomar.repository.TaskRepository;
import co.lindomar.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:lindomar;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class StaffSprint2IntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired TaskRepository tasks;
  @Autowired RoomRepository rooms;

  @Test
  void adminAssignsOnlyValidTasksToEmployees() throws Exception {
    var employee = user("empleado@lindomar.co");
    var validPayload =
        """
        {
          "id": 99999,
          "employeeId": %d,
          "title": "  Inspeccionar cuarto  ",
          "description": "  Verificar luces y minibar.  ",
          "dueDate": "%s",
          "priority": "Alta",
          "status": "DONE"
        }
        """
            .formatted(employee.getId(), LocalDate.now().plusDays(1));

    var created =
        body(
            mvc.perform(
                    post("/api/tasks")
                        .header("Authorization", auth("admin@lindomar.co"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(created.path("id").asLong()).isNotEqualTo(99999);
    assertThat(created.path("title").asText()).isEqualTo("Inspeccionar cuarto");
    assertThat(created.path("description").asText()).isEqualTo("Verificar luces y minibar.");
    assertThat(created.path("completedAt").isNull()).isTrue();
    assertThat(created.path("hasCompletionPhoto").asBoolean()).isFalse();
    assertThat(tasks.findById(created.path("id").asLong())).isPresent();

    mvc.perform(
            post("/api/tasks")
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload))
        .andExpect(status().isForbidden());
  }

  @Test
  void taskAssignmentRejectsWrongRoleUnknownEmployeeAndInvalidFields() throws Exception {
    var guest = user("huesped@lindomar.co");
    var invalidAssignee = assignmentPayload(guest.getId(), "Habitación 101", "Media");

    mvc.perform(
            post("/api/tasks")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidAssignee))
        .andExpect(status().isBadRequest());

    mvc.perform(
            post("/api/tasks")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentPayload(999999L, "Habitación 101", "Media")))
        .andExpect(status().isNotFound());

    var invalidFields =
        """
        {
          "employeeId": %d,
          "title": "   ",
          "description": "",
          "dueDate": "%s",
          "priority": "Urgente"
        }
        """
            .formatted(user("empleado@lindomar.co").getId(), LocalDate.now().minusDays(1));

    mvc.perform(
            post("/api/tasks")
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidFields))
        .andExpect(status().isBadRequest());
  }

  @Test
  void employeeViewsOnlyOwnAssignedTasks() throws Exception {
    var santiago = user("empleado@lindomar.co");
    var ana = user("ana@lindomar.co");
    var ownTask = saveTask(santiago, TaskStatus.PENDING, "Tarea privada de Santiago");
    var otherTask = saveTask(ana, TaskStatus.PENDING, "Tarea privada de Ana");

    var mine =
        body(
            mvc.perform(
                    get("/api/tasks/mine").header("Authorization", auth("empleado@lindomar.co")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(mine)
        .allSatisfy(
            task -> assertThat(task.path("employeeId").asLong()).isEqualTo(santiago.getId()));
    assertThat(ids(mine)).contains(ownTask.getId()).doesNotContain(otherTask.getId());

    mvc.perform(get("/api/tasks/mine").header("Authorization", auth("huesped@lindomar.co")))
        .andExpect(status().isForbidden());
  }

  @Test
  void employeeCanStartOwnPendingTaskButCannotSkipOrReverseTheWorkflow() throws Exception {
    var santiago = user("empleado@lindomar.co");
    var ana = user("ana@lindomar.co");
    var pending = saveTask(santiago, TaskStatus.PENDING, "Iniciar esta tarea");
    var other = saveTask(ana, TaskStatus.PENDING, "Tarea de otro empleado");

    mvc.perform(
            patch("/api/tasks/{id}/status", pending.getId())
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mvc.perform(
            patch("/api/tasks/{id}/status", pending.getId())
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isConflict());

    mvc.perform(
            patch("/api/tasks/{id}/status", pending.getId())
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PENDING\"}"))
        .andExpect(status().isConflict());

    mvc.perform(
            patch("/api/tasks/{id}/status", other.getId())
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            patch("/api/tasks/{id}/status", 999999)
                .header("Authorization", auth("empleado@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void employeeCompletesOnlyAnOwnInProgressTaskAndCannotCompleteItTwice() throws Exception {
    var santiago = user("empleado@lindomar.co");
    var ana = user("ana@lindomar.co");
    var pending = saveTask(santiago, TaskStatus.PENDING, "Aún no iniciada");
    var inProgress = saveTask(santiago, TaskStatus.IN_PROGRESS, "Lista para completar");
    var other = saveTask(ana, TaskStatus.IN_PROGRESS, "Tarea de Ana");

    mvc.perform(
            multipart("/api/tasks/{id}/complete", pending.getId())
                .header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isConflict());

    mvc.perform(
            multipart("/api/tasks/{id}/complete", other.getId())
                .header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isForbidden());

    var completed =
        body(
            mvc.perform(
                    multipart("/api/tasks/{id}/complete", inProgress.getId())
                        .header("Authorization", auth("empleado@lindomar.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(completed.path("completedAt").asText()).isNotBlank();
    assertThat(completed.path("hasCompletionPhoto").asBoolean()).isFalse();

    mvc.perform(
            multipart("/api/tasks/{id}/complete", inProgress.getId())
                .header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isConflict());

    mvc.perform(
            multipart("/api/tasks/{id}/complete", 999999)
                .header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isNotFound());
  }

  @Test
  void dashboardReportsEveryRoomStateAndTheOccupancyRate() throws Exception {
    rooms.deleteAll();
    rooms.saveAll(
        List.of(
            room("S2-1", RoomStatus.AVAILABLE),
            room("S2-2", RoomStatus.OCCUPIED),
            room("S2-3", RoomStatus.RESERVED),
            room("S2-4", RoomStatus.MAINTENANCE),
            room("S2-5", RoomStatus.OUT_OF_SERVICE)));

    mvc.perform(get("/api/dashboard").header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rooms").value(5))
        .andExpect(jsonPath("$.available").value(1))
        .andExpect(jsonPath("$.occupied").value(1))
        .andExpect(jsonPath("$.reserved").value(1))
        .andExpect(jsonPath("$.maintenance").value(1))
        .andExpect(jsonPath("$.outOfService").value(1))
        .andExpect(jsonPath("$.occupancyRate").value(20));

    mvc.perform(get("/api/dashboard").header("Authorization", auth("empleado@lindomar.co")))
        .andExpect(status().isForbidden());
  }

  @Test
  void dashboardUsesZeroOccupancyWhenThereAreNoRooms() throws Exception {
    rooms.deleteAll();

    mvc.perform(get("/api/dashboard").header("Authorization", auth("admin@lindomar.co")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rooms").value(0))
        .andExpect(jsonPath("$.available").value(0))
        .andExpect(jsonPath("$.occupied").value(0))
        .andExpect(jsonPath("$.reserved").value(0))
        .andExpect(jsonPath("$.maintenance").value(0))
        .andExpect(jsonPath("$.outOfService").value(0))
        .andExpect(jsonPath("$.occupancyRate").value(0));
  }

  private JsonNode body(String content) throws Exception {
    return json.readTree(content);
  }

  private String auth(String email) throws Exception {
    return "Bearer " + login(email).path("token").asText();
  }

  private JsonNode login(String email) throws Exception {
    var response =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"demo123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return body(response);
  }

  private UserAccount user(String email) {
    return users.findByEmailIgnoreCase(email).orElseThrow();
  }

  private StaffTask saveTask(UserAccount employee, TaskStatus status, String title) {
    return tasks.save(
        new StaffTask(
            employee.getId(),
            title,
            "Descripción de la prueba de integración.",
            status,
            LocalDate.now().plusDays(2),
            "Media"));
  }

  private String assignmentPayload(Long employeeId, String title, String priority) {
    return """
        {
          "employeeId": %d,
          "title": "%s",
          "description": "Limpieza e inspección completas.",
          "dueDate": "%s",
          "priority": "%s"
        }
        """
        .formatted(employeeId, title, LocalDate.now().plusDays(1), priority);
  }

  private List<Long> ids(JsonNode array) {
    var ids = new ArrayList<Long>();
    array.forEach(node -> ids.add(node.path("id").asLong()));
    return ids;
  }

  private Room room(String number, RoomStatus status) {
    return new Room(
        number, 9, "Prueba", 2, new BigDecimal("100000"), false, false, "Wi-Fi", status);
  }
}
