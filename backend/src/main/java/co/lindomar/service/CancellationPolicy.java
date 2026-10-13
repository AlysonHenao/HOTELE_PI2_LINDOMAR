package co.lindomar.service;

import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** La fecha límite es exclusiva y se evalúa en la zona horaria del hotel. */
@Component
public class CancellationPolicy {
    private final Clock clock;
    private final int advanceHours;
    private final LocalTime checkInTime;

    public CancellationPolicy(Clock clock, @Value("${hotel.cancellation.advance-hours:2}") int advanceHours,
            @Value("${hotel.check-in-time:15:00}") String checkInTime) {
        if (advanceHours < 0) throw new IllegalArgumentException("advance-hours no puede ser negativo");
        this.clock = clock;
        this.advanceHours = advanceHours;
        this.checkInTime = LocalTime.parse(checkInTime);
    }

    public OffsetDateTime deadline(Reservation reservation) {
        return reservation.getCheckIn().atTime(checkInTime).atZone(clock.getZone()).minusHours(advanceHours).toOffsetDateTime();
    }

    public String rejection(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) return "La reserva ya está cancelada";
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) return "Solo se pueden cancelar reservas confirmadas";
        if (!clock.instant().isBefore(deadline(reservation).toInstant())) return "El plazo de cancelación ha vencido";
        return null;
    }

    public String description() {
        return "Cancelación con reembolso completo antes del límite de " + advanceHours
            + " hora(s) previas al check-in (" + checkInTime + ", " + clock.getZone()
            + "). No se pueden cancelar estadías iniciadas o finalizadas.";
    }
}
