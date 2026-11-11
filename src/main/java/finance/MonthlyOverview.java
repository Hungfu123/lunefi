package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record MonthlyOverview(String monat,
        Map<String, BigDecimal> ausgabenJeKategorie,
        Map<String, BigDecimal> einnahmenJeKategorie,
        List<Entry> eintraege,
        Map<String, BudgetInfo> budgets) {
}
