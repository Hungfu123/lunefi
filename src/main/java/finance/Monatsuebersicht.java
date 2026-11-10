package finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record Monatsuebersicht(
        Map<String, BigDecimal> ausgabenJeKategorie,
        Map<String, BigDecimal> einnahmenJeKategorie,
        List<Eintrag> eintraege) {
}
