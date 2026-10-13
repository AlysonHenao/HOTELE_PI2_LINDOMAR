package co.lindomar.repository;

import co.lindomar.domain.Domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona acceso persistente a las cuentas de usuario. */
public interface UserRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);
}
