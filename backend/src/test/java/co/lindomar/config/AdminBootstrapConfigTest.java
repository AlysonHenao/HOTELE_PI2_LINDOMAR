package co.lindomar.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import co.lindomar.domain.Domain.Role;
import co.lindomar.domain.Domain.UserAccount;
import co.lindomar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Protege el primer acceso de AWS sin modificar cuentas en reinicios posteriores. */
class AdminBootstrapConfigTest {
  @Test
  void emptyDatabaseRequiresCredentials() {
    var users = mock(UserRepository.class);
    var command =
        new AdminBootstrapConfig().bootstrapAdmin(users, new BCryptPasswordEncoder(), "", "");
    assertThatThrownBy(() -> command.run()).isInstanceOf(IllegalStateException.class);
    verify(users, never()).save(any());
  }

  @Test
  void createsOnlyAnAdminWithHashedPassword() throws Exception {
    var users = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder();
    new AdminBootstrapConfig()
        .bootstrapAdmin(users, encoder, "Admin@example.test", "Local-test-123456")
        .run();
    var account = ArgumentCaptor.forClass(UserAccount.class);
    verify(users).save(account.capture());
    assertThat(account.getValue().getRole()).isEqualTo(Role.ADMIN);
    assertThat(account.getValue().getEmail()).isEqualTo("admin@example.test");
    assertThat(encoder.matches("Local-test-123456", account.getValue().getPassword())).isTrue();
  }

  @Test
  void restartNeverOverwritesAnExistingAccount() throws Exception {
    var users = mock(UserRepository.class);
    when(users.count()).thenReturn(1L);
    new AdminBootstrapConfig().bootstrapAdmin(users, new BCryptPasswordEncoder(), "", "").run();
    verify(users, never()).save(any());
  }
}
