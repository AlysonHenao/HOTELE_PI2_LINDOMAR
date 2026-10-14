package co.lindomar.repository;

import co.lindomar.domain.Domain.ExpenseReminder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona acceso persistente a los recordatorios de gastos. */
public interface ReminderRepository extends JpaRepository<ExpenseReminder, Long> {
  List<ExpenseReminder> findAllByOrderByDueDateAsc();
}
