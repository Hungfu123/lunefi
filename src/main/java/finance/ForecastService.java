package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class ForecastService {

    static final Set<String> FIXED_COST_CATEGORIES =
            Set.of("Miete", "Strom", "Internet", "Telefon", "Versicherung", "Abo");

    private final EntryRepository entryRepository;
    private final Clock clock;

    public ForecastService(EntryRepository entryRepository, Clock clock) {
        this.entryRepository = entryRepository;
        this.clock = clock;
    }

    public Forecast forecast(List<LineItem> entries, int monate) {
        YearMonth currentMonth = YearMonth.now(clock);
        String targetMonth = currentMonth.plusMonths(1).toString();

        List<YearMonth> baseMonths = new ArrayList<>();
        for (int back = monate; back >= 1; back--) {
            baseMonths.add(currentMonth.minusMonths(back));
        }

        if (countMonthsWithEntries(entries) < 2) {
            return new Forecast(targetMonth, Map.of(), Map.of(), BigDecimal.ZERO.setScale(2),
                    "WENIG_DATEN", baseMonths.stream().map(YearMonth::toString).toList());
        }

        Map<String, BigDecimal> expensesByCategory = forecastExpenses(entries, baseMonths);
        Map<String, BigDecimal> incomeByCategory = forecastIncome(entries, baseMonths);

        BigDecimal expenseTotal = sum(expensesByCategory);
        BigDecimal incomeTotal = sum(incomeByCategory);
        BigDecimal difference = incomeTotal.subtract(expenseTotal).setScale(2, RoundingMode.HALF_UP);

        return new Forecast(targetMonth, expensesByCategory, incomeByCategory, difference, "OK",
                baseMonths.stream().map(YearMonth::toString).toList());
    }

    private long countMonthsWithEntries(List<LineItem> entries) {
        return entries.stream()
                .map(item -> YearMonth.from(item.date()))
                .distinct()
                .count();
    }

    private Map<String, BigDecimal> forecastExpenses(List<LineItem> entries,
            List<YearMonth> baseMonths) {
        Map<String, Map<YearMonth, BigDecimal>> monthlySums = sumsByCategoryAndMonth(entries,
                baseMonths, false);

        Map<String, BigDecimal> predicted = new LinkedHashMap<>();
        for (Map.Entry<String, Map<YearMonth, BigDecimal>> category : monthlySums.entrySet()) {
            if (FIXED_COST_CATEGORIES.contains(category.getKey())) {
                BigDecimal lastKnownValue = lastKnownValue(category.getValue(), baseMonths);
                if (lastKnownValue != null) {
                    predicted.put(category.getKey(), lastKnownValue);
                }
            } else {
                predicted.put(category.getKey(), median(monthlySumsWithZeros(category.getValue(),
                        baseMonths)));
            }
        }
        return predicted;
    }

    private BigDecimal lastKnownValue(Map<YearMonth, BigDecimal> monthlySums,
            List<YearMonth> baseMonths) {
        for (int i = baseMonths.size() - 1; i >= 0; i--) {
            BigDecimal value = monthlySums.get(baseMonths.get(i));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<String, BigDecimal> forecastIncome(List<LineItem> entries,
            List<YearMonth> baseMonths) {
        Map<String, Map<YearMonth, BigDecimal>> monthlySums = sumsByCategoryAndMonth(entries,
                baseMonths, true);

        Map<String, BigDecimal> predicted = new LinkedHashMap<>();
        for (Map.Entry<String, Map<YearMonth, BigDecimal>> category : monthlySums.entrySet()) {
            if (!isRegular(category.getValue())) {
                continue;
            }
            predicted.put(category.getKey(), median(new ArrayList<>(category.getValue().values())));
        }
        return predicted;
    }

    private boolean isRegular(Map<YearMonth, BigDecimal> monthlySums) {
        if (monthlySums.size() < 2) {
            return false;
        }
        BigDecimal largest = monthlySums.values().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal smallest = monthlySums.values().stream()
                .min(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal range = largest.subtract(smallest);
        return range.compareTo(largest.multiply(new BigDecimal("0.5"))) <= 0;
    }

    private Map<String, Map<YearMonth, BigDecimal>> sumsByCategoryAndMonth(List<LineItem> entries,
            List<YearMonth> baseMonths, boolean income) {
        Map<String, Map<YearMonth, BigDecimal>> sums = new LinkedHashMap<>();
        for (LineItem item : entries) {
            YearMonth month = YearMonth.from(item.date());
            if (!baseMonths.contains(month)) {
                continue;
            }
            if (income != item.amount().signum() >= 0) {
                continue;
            }
            sums.computeIfAbsent(item.category(), category -> new LinkedHashMap<>())
                    .merge(month, item.amount().abs(), BigDecimal::add);
        }
        return sums;
    }

    private List<BigDecimal> monthlySumsWithZeros(Map<YearMonth, BigDecimal> monthlySums,
            List<YearMonth> baseMonths) {
        List<BigDecimal> values = new ArrayList<>();
        for (YearMonth month : baseMonths) {
            values.add(monthlySums.getOrDefault(month, BigDecimal.ZERO));
        }
        return values;
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle).setScale(2, RoundingMode.HALF_UP);
        }
        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(Map<String, BigDecimal> valuesByCategory) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : valuesByCategory.values()) {
            total = total.add(value);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
