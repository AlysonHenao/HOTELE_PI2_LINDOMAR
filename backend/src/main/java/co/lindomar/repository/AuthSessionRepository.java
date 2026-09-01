package co.lindomar.repository;

import co.lindomar.domain.Domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession,String>{
 Optional<AuthSession> findByToken(String token);
 void deleteByToken(String token);
}
