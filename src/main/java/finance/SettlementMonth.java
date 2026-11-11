package finance;

import java.math.BigDecimal;
import java.util.Map;

public record SettlementMonth(String monat, boolean laeuftNoch, boolean ausgeglichen,
        Map<String, BigDecimal> saldi, String vorschlag) {
}
