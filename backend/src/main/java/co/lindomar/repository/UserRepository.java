package co.lindomar.repository;
import co.lindomar.domain.Domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long>{
 Optional<UserAccount> findByEmailIgnoreCase(String email);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select u from UserAccount u order by u.id")
 java.util.List<UserAccount> findAllForUpdate();
}

