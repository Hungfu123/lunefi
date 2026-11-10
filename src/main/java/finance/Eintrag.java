package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Eintrag(LocalDate datum, BigDecimal betrag, String kategorie) {
}
