package co.lindomar.repository;

import co.lindomar.domain.Domain.StaffTask;
import co.lindomar.domain.Domain.TaskStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Define la operación para la gestión del hotel. */
public interface TaskRepository extends JpaRepository<StaffTask, Long> {
  List<StaffTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);

  long countByEmployeeIdAndStatusNot(Long employeeId, TaskStatus status);

  boolean existsByEventKey(String eventKey);

  Optional<StaffTask> findByEventKey(String eventKey);

  /** Serializa cambios de responsable y de estado de una misma tarea. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<StaffTask> findLockedById(Long id);
}
