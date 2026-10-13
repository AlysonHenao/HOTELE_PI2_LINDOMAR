package co.lindomar.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HotelClockConfig {
    @Bean
    Clock hotelClock(@Value("${hotel.time-zone:America/Bogota}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
