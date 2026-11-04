package co.lindomar.web;

import co.lindomar.domain.Domain.EntryType;
import co.lindomar.domain.Domain.ReminderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Contratos de entrada: los identificadores y pagos siempre los decide el servidor. */
public final class FinanceRequests {
  private FinanceRequests() {}

  /** Movimiento manual con valor positivo expresado en pesos colombianos. */
  public record Movement(
      @NotNull(message = "Selecciona ingreso o gasto") EntryType type,
      @NotBlank(message = "El concepto es obligatorio") @Size(max = 255) String concept,
      @NotNull(message = "El valor es obligatorio")
          @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero")
          @Digits(integer = 12, fraction = 2, message = "Usa hasta 12 enteros y 2 decimales")
          BigDecimal amount,
      LocalDate date) {
    public Movement {
      concept = concept == null ? null : concept.trim();
    }
  }

  /** Creación de un recordatorio pendiente; nunca crea un gasto por sí sola. */
  public record Reminder(
      @NotBlank(message = "El concepto es obligatorio") @Size(max = 255) String concept,
      @NotBlank(message = "La categoría es obligatoria") @Size(max = 255) String category,
      @NotNull(message = "El valor estimado es obligatorio")
          @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero")
          @Digits(integer = 12, fraction = 2, message = "Usa hasta 12 enteros y 2 decimales")
          BigDecimal estimatedAmount,
      @NotNull(message = "La fecha límite es obligatoria") LocalDate dueDate) {
    public Reminder {
      concept = concept == null ? null : concept.trim();
      category = category == null ? null : category.trim();
    }
  }

  /** Edición parcial de datos; el estado DONE solo se alcanza por el endpoint de pago. */
  public record ReminderUpdate(
      @Size(min = 1, max = 255) String concept,
      @Size(min = 1, max = 255) String category,
      @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero")
          @Digits(integer = 12, fraction = 2, message = "Usa hasta 12 enteros y 2 decimales")
          BigDecimal estimatedAmount,
      LocalDate dueDate,
      ReminderStatus status) {
    public ReminderUpdate {
      concept = concept == null ? null : concept.trim();
      category = category == null ? null : category.trim();
    }
  }
}
