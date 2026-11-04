package co.lindomar.repository;

import co.lindomar.domain.Domain.ServiceRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Proporciona acceso persistente a las solicitudes de servicio. */
public interface RequestRepository extends JpaRepository<ServiceRequest, Long> {
  List<ServiceRequest> findByGuestIdOrderByCreatedAtDesc(Long guestId);

  Optional<ServiceRequest> findByClientRequestId(UUID clientRequestId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ServiceRequest r where r.id = :id")
  Optional<ServiceRequest> findByIdForUpdate(@Param("id") Long id);
}
