package co.lindomar.repository;

import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona consultas para administrar y validar reservas. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
  List<Reservation> findByGuestIdOrderByCheckInDesc(Long guestId);

  boolean existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
      Long roomId, ReservationStatus status, LocalDate checkOut, LocalDate checkIn);
}
