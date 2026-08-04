package co.lindomar.repository;
import co.lindomar.domain.Domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long>{Optional<UserAccount> findByEmailIgnoreCase(String email);}

