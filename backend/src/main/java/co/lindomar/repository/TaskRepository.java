package co.lindomar.repository;
import co.lindomar.domain.Domain.StaffTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskRepository extends JpaRepository<StaffTask,Long>{List<StaffTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);}

