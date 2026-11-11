package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Sparziel(Long id, String name, BigDecimal zielbetrag,
        BigDecimal eingezahlt, Boolean erreicht, List<Einzahlung> einzahlungen) {
}
