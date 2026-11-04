package co.lindomar.repository;

import co.lindomar.domain.TaskNotification;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Almacena y consulta los avisos de cada empleado. */
public interface TaskNotificationRepository extends JpaRepository<TaskNotification, Long> {
  List<TaskNotification> findTop50ByEmployeeIdAndReadAtIsNullOrderByIdDesc(Long employeeId);

  /** Marca el aviso como leído sin modificar avisos de otros empleados. */
  @Modifying
  @Query(
      """
      update TaskNotification n set n.readAt = :readAt
      where n.id = :id and n.employeeId = :employeeId and n.readAt is null
      """)
  void markRead(Long id, Long employeeId, OffsetDateTime readAt);

  boolean existsByIdAndEmployeeId(Long id, Long employeeId);
}
