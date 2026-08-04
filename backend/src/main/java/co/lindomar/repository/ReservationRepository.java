package co.lindomar.repository;
import co.lindomar.domain.Domain.Reservation;
import co.lindomar.domain.Domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface ReservationRepository extends JpaRepository<Reservation,Long>{
 List<Reservation> findByGuestIdOrderByCheckInDesc(Long guestId);
 boolean existsByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(Long roomId, ReservationStatus status, LocalDate checkOut, LocalDate checkIn);
}

