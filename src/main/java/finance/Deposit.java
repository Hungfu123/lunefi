package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Deposit(Long id, BigDecimal betrag, String person, LocalDate datum) {
}
