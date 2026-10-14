package co.lindomar.repository;

import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Define la operación para la gestión del hotel. */
public interface UserRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  List<UserAccount> findByRoleOrderByIdAsc(Role role);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from UserAccount u order by u.id")
  java.util.List<UserAccount> findAllForUpdate();
}
