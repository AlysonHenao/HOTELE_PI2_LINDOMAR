package co.lindomar.repository;

import co.lindomar.domain.Domain.ServiceRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona acceso persistente a las solicitudes de servicio. */
public interface RequestRepository extends JpaRepository<ServiceRequest, Long> {
  List<ServiceRequest> findByGuestIdOrderByCreatedAtDesc(Long guestId);
}
