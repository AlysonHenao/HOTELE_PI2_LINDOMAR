package co.lindomar.repository;

import co.lindomar.domain.Domain.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proporciona acceso persistente a las sesiones de autenticación. */
public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
  Optional<AuthSession> findByToken(String token);

  void deleteByToken(String token);
}
