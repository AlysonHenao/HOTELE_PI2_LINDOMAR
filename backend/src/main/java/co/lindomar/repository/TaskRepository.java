package co.lindomar.repository;

import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Define la operación para la gestión del hotel. */
public interface TaskRepository extends JpaRepository<StaffTask, Long> {
  List<StaffTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);

  long countByEmployeeIdAndStatusNot(Long employeeId, TaskStatus status);

  boolean existsByEventKey(String eventKey);
}
