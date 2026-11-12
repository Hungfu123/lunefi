package finance;

import java.math.BigDecimal;

public record YearStatistics(int jahr, java.util.List<HistoryMonth> monate,
        java.util.Map<String, BigDecimal> ausgabenJeKategorie,
        java.util.Map<String, BigDecimal> einnahmenJeKategorie) {
}
