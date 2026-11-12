package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineItem(Long entryId, Long partId, LocalDate date, BigDecimal amount,
        String category, String person, boolean shared) {
}
