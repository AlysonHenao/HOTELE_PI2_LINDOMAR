package co.lindomar;

import co.lindomar.domain.Domain.*;
import co.lindomar.service.CancellationPolicy;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class CancellationPolicyTest {
    @ParameterizedTest
    @CsvSource({"2030-06-10T17:59:59Z,true", "2030-06-10T18:00:00Z,false", "2030-06-10T18:00:01Z,false", "2030-06-11T00:00:00Z,false"})
    void twoHourDeadlineUsesHotelTimezoneAndRejectsAtTheCutoff(String instant, boolean allowed) {
        var clock = Clock.fixed(Instant.parse(instant), ZoneId.of("America/Bogota"));
        var policy = new CancellationPolicy(clock, 2, "15:00");
        var reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2030, 6, 10)); reservation.setStatus(ReservationStatus.CONFIRMED);
        assertThat(policy.deadline(reservation)).isEqualTo(OffsetDateTime.parse("2030-06-10T13:00:00-05:00"));
        assertThat(policy.rejection(reservation) == null).isEqualTo(allowed);
    }
}
