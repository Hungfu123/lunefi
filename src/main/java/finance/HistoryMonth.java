package finance;

import java.math.BigDecimal;

public record HistoryMonth(String monat, BigDecimal einnahmen, BigDecimal ausgaben, BigDecimal differenz) {
}
