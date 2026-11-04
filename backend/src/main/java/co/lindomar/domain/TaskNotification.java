package co.lindomar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Aviso persistente para el empleado afectado por una asignación. */
@Entity
@Table(name = "task_notifications", indexes = @Index(columnList = "employee_id,read_at,id"))
public class TaskNotification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Column(nullable = false)
  private Long taskId;

  @Column(nullable = false, length = 320)
  private String message;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  protected TaskNotification() {}

  /** Crea un aviso aún no leído. */
  public TaskNotification(Long employeeId, Long taskId, String message, OffsetDateTime createdAt) {
    this.employeeId = employeeId;
    this.taskId = taskId;
    this.message = message;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getTaskId() {
    return taskId;
  }

  public String getMessage() {
    return message;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
