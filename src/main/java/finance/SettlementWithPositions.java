package finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SettlementWithPositions(String monat, boolean laeuftNoch, boolean ausgeglichen,
        Map<String, BigDecimal> saldi, String vorschlag, List<SettlementPosition> positionen) {
}
