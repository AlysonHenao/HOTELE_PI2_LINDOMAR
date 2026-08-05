package co.lindomar.repository;
import co.lindomar.domain.Domain.ExpenseReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReminderRepository extends JpaRepository<ExpenseReminder,Long>{
 List<ExpenseReminder> findAllByOrderByDueDateAsc();
}
