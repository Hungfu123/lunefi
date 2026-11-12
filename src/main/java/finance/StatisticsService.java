package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private static final int SAVINGS_POTENTIAL_BASE_MONTHS = 3;
    private static final BigDecimal HINT_THRESHOLD = new BigDecimal("10.00");

    private final Clock clock;

    public StatisticsService(Clock clock) {
        this.clock = clock;
    }

    public CategoryStatistics categoryHistory(List<LineItem> entries, String category, int monate) {
        YearMonth currentMonth = YearMonth.now(clock);

        List<StatisticsMonth> trend = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (int back = monate - 1; back >= 0; back--) {
            YearMonth month = currentMonth.minusMonths(back);
            BigDecimal amount = expensesInMonth(entries, category, month);
            values.add(amount);
            sum = sum.add(amount);
            trend.add(new StatisticsMonth(month.toString(), amount));
        }

        BigDecimal median = ForecastService.median(values);
        BigDecimal average = sum.divide(new BigDecimal(monate), 2, RoundingMode.HALF_UP);
        return new CategoryStatistics(category, trend, median, average);
    }

    public SavingsPotential savingsPotential(List<LineItem> entries, YearMonth monat) {
        long monthsWithEntries = entries.stream()
                .map(item -> YearMonth.from(item.date()))
                .filter(earlier -> earlier.isBefore(monat))
                .distinct()
                .count();
        if (monthsWithEntries < 2) {
            return new SavingsPotential(List.of(), "WENIG_DATEN");
        }

        List<YearMonth> baseMonths = new ArrayList<>();
        for (int back = SAVINGS_POTENTIAL_BASE_MONTHS; back >= 1; back--) {
            baseMonths.add(monat.minusMonths(back));
        }

        Map<String, BigDecimal> actualByCategory = expensesByCategory(entries, monat);
        List<SavingsHint> hints = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> category : actualByCategory.entrySet()) {
            if (ForecastService.FIXED_COST_CATEGORIES.contains(category.getKey())) {
                continue;
            }
            BigDecimal actual = category.getValue();
            BigDecimal usual = usualExpenses(entries, category.getKey(), baseMonths);
            if (usual.signum() <= 0 || actual.compareTo(usual) <= 0
                    || actual.subtract(usual).compareTo(HINT_THRESHOLD) < 0) {
                continue;
            }
            hints.add(new SavingsHint(category.getKey(), actual, usual,
                    hintText(category.getKey(), actual, usual)));
        }
        return new SavingsPotential(hints, "OK");
    }

    private BigDecimal expensesInMonth(List<LineItem> entries, String category, YearMonth month) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LineItem item : entries) {
            if (YearMonth.from(item.date()).equals(month)
                    && item.category().equals(category)
                    && item.amount().signum() < 0) {
                sum = sum.add(item.amount().abs());
            }
        }
        return sum.setScale(2);
    }

    private Map<String, BigDecimal> expensesByCategory(List<LineItem> entries, YearMonth monat) {
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        for (LineItem item : entries) {
            if (!YearMonth.from(item.date()).equals(monat) || item.amount().signum() >= 0) {
                continue;
            }
            sums.merge(item.category(), item.amount().abs(), BigDecimal::add);
        }
        return sums;
    }

    private BigDecimal usualExpenses(List<LineItem> entries, String category,
            List<YearMonth> baseMonths) {
        List<BigDecimal> values = new ArrayList<>();
        for (YearMonth baseMonth : baseMonths) {
            values.add(expensesInMonth(entries, category, baseMonth));
        }
        return ForecastService.median(values);
    }

    private String hintText(String category, BigDecimal actual, BigDecimal usual) {
        return category + ": " + wholeEuros(actual) + " € — sonst meist " + wholeEuros(usual) + " €.";
    }

    private String wholeEuros(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
