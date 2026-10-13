package co.lindomar.repository;

import co.lindomar.domain.Domain.StaffTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona acceso persistente a las tareas operativas del personal. */
public interface TaskRepository extends JpaRepository<StaffTask, Long> {
  List<StaffTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);
}
