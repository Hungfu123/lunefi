package finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record Forecast(String monat, Map<String, BigDecimal> ausgabenJeKategorie,
        Map<String, BigDecimal> einnahmenJeKategorie, BigDecimal differenz, String konfidenz,
        List<String> basisMonate) {
}
