package finance.bdd;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AdjustableClock extends java.time.Clock {

    private volatile Instant now = Instant.now();
    private final ZoneId zone = ZoneId.systemDefault();

    public void setTo(YearMonth monat) {
        now = ZonedDateTime.of(monat.atDay(1).atTime(12, 0), zone).toInstant();
    }

    public void reset() {
        now = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public java.time.Clock withZone(ZoneId zone) {
        return this;
    }
}
