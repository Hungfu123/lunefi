package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Eintrag(Long id, LocalDate datum, BigDecimal betrag, String kategorie,
        String person, Boolean gemeinsam, String zahler, String konto,
        String verwendungszweck, Long quelleId) {
}
