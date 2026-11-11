package finance;

import java.math.BigDecimal;

public record VerlaufMonat(String monat, BigDecimal einnahmen, BigDecimal ausgaben, BigDecimal differenz) {
}
