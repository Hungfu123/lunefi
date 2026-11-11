package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record Monatsuebersicht(String monat,
        Map<String, BigDecimal> ausgabenJeKategorie,
        Map<String, BigDecimal> einnahmenJeKategorie,
        List<Eintrag> eintraege,
        Map<String, BudgetInfo> budgets) {
}
