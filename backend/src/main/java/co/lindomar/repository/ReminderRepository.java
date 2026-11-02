package co.lindomar.repository;

import co.lindomar.domain.Domain.ExpenseReminder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Proporciona acceso persistente a los recordatorios de gastos. */
public interface ReminderRepository extends JpaRepository<ExpenseReminder, Long> {
  List<ExpenseReminder> findAllByOrderByDueDateAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ExpenseReminder r where r.id = :id")
  Optional<ExpenseReminder> findByIdForUpdate(@Param("id") Long id);
}
