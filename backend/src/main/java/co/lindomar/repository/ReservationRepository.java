package co.lindomar.repository;

import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Define la operación para la gestión del hotel. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
  boolean existsByRoomIdAndStatusInAndCheckOutGreaterThan(
      Long roomId, java.util.Collection<ReservationStatus> statuses, LocalDate date);

  boolean existsByRoomId(Long roomId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reservation r where r.id = :id")
  Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

  boolean existsByRoomIdAndIdNotAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
      Long roomId, Long id, ReservationStatus status, LocalDate checkOut, LocalDate checkIn);

  List<Reservation> findByGuestIdOrderByCheckInDesc(Long guestId);

  boolean existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
      Long roomId, ReservationStatus status, LocalDate checkOut, LocalDate checkIn);
}
