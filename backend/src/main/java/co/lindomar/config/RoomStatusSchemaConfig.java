package co.lindomar.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RoomStatusSchemaConfig {
    // Hibernate ddl-auto=update does not extend PostgreSQL's existing enum CHECK.
    // Preserve prototype volumes while allowing the two additional room states.
    @Bean
    @Order(-1)
    CommandLineRunner extendRoomStatusConstraint(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        return args -> {
            try (var connection = jdbc.getDataSource().getConnection()) {
                if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) return;
            }
            new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                var definitions = jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                    + "WHERE conrelid = 'rooms'::regclass AND conname = 'rooms_status_check'", String.class);
                if (definitions.size() == 1 && definitions.getFirst().contains("RESERVED")
                    && definitions.getFirst().contains("OUT_OF_SERVICE")) return;
                jdbc.execute("ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_status_check");
                jdbc.execute("ALTER TABLE rooms ADD CONSTRAINT rooms_status_check CHECK "
                    + "(status IN ('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE','OUT_OF_SERVICE'))");
            });
        };
    }
}
