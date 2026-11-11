package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SavingsGoal(Long id, String name, BigDecimal zielbetrag,
        BigDecimal eingezahlt, Boolean erreicht, List<Deposit> einzahlungen) {
}
