package co.lindomar.web;

import co.lindomar.domain.Domain.RequestStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Entradas de huéspedes: nunca permiten asignar propietario, estado ni importe persistido. */
public final class GuestStayRequests {
  private GuestStayRequests() {}

  /** expectedTotal confirma el precio de la cotización; se exige al guardar el cambio. */
  public record Change(
      @NotNull(message = "Selecciona una habitación") Long roomId,
      @NotNull(message = "Selecciona la llegada") LocalDate checkIn,
      @NotNull(message = "Selecciona la salida") LocalDate checkOut,
      @Min(value = 1, message = "Indica al menos un huésped") int guests,
      @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres") String notes,
      @NotNull(message = "Recarga la reserva antes de modificarla") @Min(0) Long expectedVersion,
      @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal expectedTotal) {
    public Change {
      notes = notes == null ? null : notes.trim();
    }
  }

  /** La habitación se obtiene de una estadía propia vigente, no de texto libre. */
  public record Assistance(
      @NotNull(message = "Selecciona tu estadía") Long reservationId,
      @NotBlank(message = "Selecciona el tipo de solicitud")
          @Pattern(
              regexp = "Toallas y lencería|Limpieza|Alimentos y bebidas|Mantenimiento|Otro",
              message = "Selecciona un tipo de solicitud válido")
          String category,
      @NotBlank(message = "Describe qué necesitas")
          @Size(max = 1200, message = "La descripción no puede superar 1200 caracteres")
          String notes,
      @NotNull(message = "Falta el identificador de la solicitud; vuelve a intentarlo")
          UUID clientRequestId) {
    public Assistance {
      category = category == null ? null : category.trim();
      notes = notes == null ? null : notes.trim();
    }
  }

  /** El personal puede avanzar la atención; no editar al huésped ni su habitación. */
  public record AssistanceStatus(@NotNull(message = "Selecciona un estado") RequestStatus status) {}
}
