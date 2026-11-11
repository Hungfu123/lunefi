package finance;

import java.math.BigDecimal;

public record Budget(String monat, String kategorie, BigDecimal limit) {
}
