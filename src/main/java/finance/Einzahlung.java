package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Einzahlung(Long id, BigDecimal betrag, String person, LocalDate datum) {
}
