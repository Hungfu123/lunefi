package finance;

import java.math.BigDecimal;

public record CategoryStatistics(String kategorie, java.util.List<StatisticsMonth> monate,
        BigDecimal median, BigDecimal durchschnitt) {
}
