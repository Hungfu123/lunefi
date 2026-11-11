package finance;

import java.math.BigDecimal;
import java.util.Map;

public record SettlementPosition(Long id, String kategorie, BigDecimal betrag,
        String zahler, Map<String, BigDecimal> anteile) {
}
