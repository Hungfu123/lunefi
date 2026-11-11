package finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AusgleichMitPositionen(String monat, boolean laeuftNoch,
        Map<String, BigDecimal> saldi, String vorschlag, List<AusgleichPosition> positionen) {
}
