package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Entry(Long id, LocalDate datum, BigDecimal betrag, String kategorie,
        String person, Boolean gemeinsam, String zahler, String konto,
        String verwendungszweck, Long quelleId, List<EntryPart> teile) {

    public Entry(Long id, LocalDate datum, BigDecimal betrag, String kategorie,
            String person, Boolean gemeinsam, String zahler, String konto,
            String verwendungszweck, Long quelleId) {
        this(id, datum, betrag, kategorie, person, gemeinsam, zahler, konto,
                verwendungszweck, quelleId, null);
    }
}
