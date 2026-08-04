package co.lindomar.repository;
import co.lindomar.domain.Domain.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RequestRepository extends JpaRepository<ServiceRequest,Long>{List<ServiceRequest> findByGuestIdOrderByCreatedAtDesc(Long guestId);}

