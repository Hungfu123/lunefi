package finance.bdd;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class SetzbareUhr extends java.time.Clock {

    private volatile Instant jetzt = Instant.now();
    private final ZoneId zone = ZoneId.systemDefault();

    public void auf(YearMonth monat) {
        jetzt = ZonedDateTime.of(monat.atDay(1).atTime(12, 0), zone).toInstant();
    }

    public void zuruecksetzen() {
        jetzt = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Instant instant() {
        return jetzt;
    }

    @Override
    public java.time.Clock withZone(ZoneId zone) {
        return this;
    }
}
