package finance;

import java.math.BigDecimal;

public record EntryPart(BigDecimal betrag, String kategorie, String person, Boolean gemeinsam) {
}
