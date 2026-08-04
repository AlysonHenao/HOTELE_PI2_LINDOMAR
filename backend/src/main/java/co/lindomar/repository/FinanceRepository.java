package co.lindomar.repository;
import co.lindomar.domain.Domain.FinanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FinanceRepository extends JpaRepository<FinanceEntry,Long>{}

