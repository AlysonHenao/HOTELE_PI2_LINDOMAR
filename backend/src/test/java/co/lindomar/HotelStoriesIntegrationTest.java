package co.lindomar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:lindomar;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HotelStoriesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode login(String email) throws Exception {
        var response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"demo123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test
    void logoutRevokesTheServerSession() throws Exception {
        var token = login("huesped@lindomar.co").get("token").asText();

        mvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completesReservationViewingAndPhotoEvidenceStories() throws Exception {
        var guestSession = login("huesped@lindomar.co");
        var token = guestSession.get("token").asText();
        var guestId = guestSession.path("user").path("id").asLong();
        var rooms = json.readTree(mvc.perform(get("/api/rooms"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var roomId = rooms.get(0).get("id").asLong();
        var checkIn = LocalDate.now().plusDays(10);
        var checkOut = checkIn.plusDays(2);
        var payload = "{\"guestId\":999999,\"roomId\":" + roomId + ",\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\",\"guests\":1}";

        var createdText = mvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var created = json.readTree(createdText);
        assertThat(created.get("guestId").asLong()).isEqualTo(guestId);
        assertThat(created.get("total").decimalValue()).isPositive();

        mvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isConflict());

        var mine = json.readTree(mvc.perform(get("/api/reservations/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(mine.size()).isEqualTo(1);

        var adminReservations = json.readTree(mvc.perform(get("/api/reservations"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(adminReservations.get(0).get("guestName").asText()).isEqualTo("Mariana Torres");

        var employee = login("empleado@lindomar.co");
        var employeeId = employee.path("user").path("id").asLong();
        var employeeToken = employee.get("token").asText();
        var taskList = json.readTree(mvc.perform(get("/api/tasks").param("employeeId", String.valueOf(employeeId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var taskId = taskList.get(0).get("id").asLong();
        var photo = new MockMultipartFile("photo", "evidencia.png", "image/png", new byte[]{1,2,3,4,5});
        var completedText = mvc.perform(multipart("/api/tasks/{id}/complete", taskId).file(photo)
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var completed = json.readTree(completedText);
        assertThat(completed.get("status").asText()).isEqualTo("DONE");
        assertThat(completed.get("hasCompletionPhoto").asBoolean()).isTrue();
        assertThat(completed.get("completionPhotoName").asText()).isEqualTo("evidencia.png");

        var adminToken = login("admin@lindomar.co").get("token").asText();
        var photoResponse = mvc.perform(get("/api/tasks/{id}/photo", taskId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(photoResponse.getContentType()).isEqualTo("image/png");
        assertThat(photoResponse.getContentAsByteArray()).containsExactly(1,2,3,4,5);
    }
}
