package co.lindomar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.lindomar.domain.Domain.RequestStatus;
import co.lindomar.domain.Domain.ServiceRequest;
import co.lindomar.repository.RequestRepository;
import co.lindomar.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integración para historias de usuario complementarias: - HU-05: Filtrado de
 * habitaciones por servicio y criterios combinados. - HU-13: Adición de comentarios a solicitudes
 * de asistencia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:remaining_stories;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class RemainingStoriesIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private RequestRepository requests;
  @Autowired private UserRepository users;

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

  @Test
  void hu05CanFilterRoomsByServiceAndMultipleFilters() throws Exception {
    // Filtrar por servicio 'Jacuzzi'
    mvc.perform(get("/api/rooms").param("service", "Jacuzzi"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(
            jsonPath("$[0].services").value(org.hamcrest.Matchers.containsString("Jacuzzi")));

    // Filtrar por servicio inexistente retorna lista vacía
    mvc.perform(get("/api/rooms").param("service", "ServicioInexistente12345"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // Filtros combinados: tipo Deluxe y servicio Smart TV
    mvc.perform(get("/api/rooms").param("type", "Deluxe").param("service", "Smart TV"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void hu13GuestCanAddCommentsToTheirOwnAssistanceRequest() throws Exception {
    var guest = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow();

    var req = new ServiceRequest();
    req.setGuestId(guest.getId());
    req.setReservationId(1L);
    req.setClientRequestId(UUID.randomUUID());
    req.setRoomNumber("101");
    req.setCategory("Mantenimiento");
    req.setNotes("La lámpara no enciende");
    req.setStatus(RequestStatus.OPEN);
    req.setCreatedAt(LocalDateTime.now());
    req = requests.save(req);

    var commentInput = Map.of("comment", "Por favor revisar antes de las 4 PM");

    mvc.perform(
            post("/api/requests/{id}/comments", req.getId())
                .header("Authorization", auth("huesped@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(commentInput)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.notes")
                .value(org.hamcrest.Matchers.containsString("Por favor revisar antes de las 4 PM")))
        .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("Huésped")));

    var updated = requests.findById(req.getId()).orElseThrow();
    assertThat(updated.getNotes()).contains("Por favor revisar antes de las 4 PM");
  }

  @Test
  void hu13ValidatesCommentInputAndRejectsForeignGuest() throws Exception {
    var guest = users.findByEmailIgnoreCase("huesped@lindomar.co").orElseThrow();

    var req = new ServiceRequest();
    req.setGuestId(guest.getId() + 9999L); // de otro huésped
    req.setReservationId(1L);
    req.setClientRequestId(UUID.randomUUID());
    req.setRoomNumber("101");
    req.setCategory("Limpieza");
    req.setNotes("Toallas");
    req.setStatus(RequestStatus.OPEN);
    req.setCreatedAt(LocalDateTime.now());
    req = requests.save(req);

    // Huésped ajeno no puede comentar
    mvc.perform(
            post("/api/requests/{id}/comments", req.getId())
                .header("Authorization", auth("huesped@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("comment", "Intento no autorizado"))))
        .andExpect(status().isForbidden());

    // Comentario vacío es rechazado
    mvc.perform(
            post("/api/requests/{id}/comments", req.getId())
                .header("Authorization", auth("admin@lindomar.co"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("comment", "   "))))
        .andExpect(status().isBadRequest());
  }
}
