package co.lindomar.config;

import co.lindomar.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Upgrade prototype databases before accepting logins; do not rehash on restart.
    @Bean
    @Order(0)
    CommandLineRunner migrateLegacyPasswords(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            for (var user : users.findAll()) {
                var password = user.getPassword();
                if (password != null && !password.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
                    user.setPassword(encoder.encode(password));
                    users.save(user);
                }
            }
        };
    }
}
