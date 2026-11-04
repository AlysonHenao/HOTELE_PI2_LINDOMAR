package co.lindomar.config;

import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Inicializa una base nueva de AWS sin habilitar las cuentas públicas de demostración. */
@Configuration
@ConditionalOnProperty(name = "hotel.demo-data.enabled", havingValue = "false")
public class AdminBootstrapConfig {
  @Bean
  CommandLineRunner bootstrapAdmin(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${hotel.bootstrap.email:}") String email,
      @Value("${hotel.bootstrap.password:}") String password) {
    return args -> {
      if (users.count() > 0) {
        return;
      }
      if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
          || email.length() > 255
          || password.length() < 12
          || password.getBytes(StandardCharsets.UTF_8).length > 72) {
        throw new IllegalStateException(
            "Configura HOTEL_ADMIN_EMAIL y HOTEL_ADMIN_PASSWORD con una clave válida");
      }
      users.save(
          new UserAccount(
              "Administrador",
              email.trim().toLowerCase(Locale.ROOT),
              encoder.encode(password),
              Role.ADMIN,
              ""));
    };
  }
}
