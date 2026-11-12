package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SettlementService {

    private final EntryRepository entryRepository;
    private final EntryParts entryParts;
    private final PersonRepository personRepository;
    private final CategoryRepository categoryRepository;
    private final SettledMonthRepository settledMonthRepository;
    private final java.time.Clock clock;

    public SettlementService(EntryRepository entryRepository, EntryParts entryParts,
            PersonRepository personRepository, CategoryRepository categoryRepository,
            SettledMonthRepository settledMonthRepository, java.time.Clock clock) {
        this.entryRepository = entryRepository;
        this.entryParts = entryParts;
        this.personRepository = personRepository;
        this.categoryRepository = categoryRepository;
        this.settledMonthRepository = settledMonthRepository;
        this.clock = clock;
    }

    public SettlementWithPositions settlement(YearMonth month) {
        List<String> persons = personRepository.findAll().stream()
                .map(PersonEntity::getName)
                .sorted()
                .toList();

        Map<String, BigDecimal> saldi = new LinkedHashMap<>();
        for (String person : persons) {
            saldi.put(person, BigDecimal.ZERO.setScale(2));
        }

        List<EntryEntity> entitiesInMonth = entryRepository.findAll().stream()
                .filter(entry -> YearMonth.from(entry.getDatum()).equals(month))
                .sorted(Comparator.comparing(EntryEntity::getDatum)
                        .thenComparing(EntryEntity::getId))
                .toList();
        Map<Long, String> payersByEntry = new LinkedHashMap<>();
        for (EntryEntity entry : entitiesInMonth) {
            payersByEntry.put(entry.getId(), entry.getZahler());
        }

        List<SettlementPosition> positions = new ArrayList<>();
        for (LineItem item : entryParts.lineItems(entitiesInMonth)) {
            if (!item.shared()) {
                continue;
            }
            Map<String, BigDecimal> shares = computeShares(
                    item.amount().abs().setScale(2, RoundingMode.HALF_UP),
                    percentagesFor(item.category(), persons));
            Long positionId = item.partId() != null ? item.partId() : item.entryId();
            positions.add(new SettlementPosition(positionId, item.category(),
                    item.amount(), payersByEntry.get(item.entryId()), shares));

            BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            int sign = item.amount().signum() < 0 ? 1 : -1;
            String payer = payersByEntry.get(item.entryId());
            for (Map.Entry<String, BigDecimal> share : shares.entrySet()) {
                BigDecimal advancedForOthers = share.getKey().equals(payer)
                        ? total.subtract(share.getValue())
                        : share.getValue().negate();
                saldi.merge(share.getKey(), advancedForOthers.multiply(
                        BigDecimal.valueOf(sign)), BigDecimal::add);
            }
        }

        boolean stillRunning = month.equals(YearMonth.now(clock));
        return new SettlementWithPositions(month.toString(), stillRunning,
                settledMonthRepository.existsByMonat(month.toString()), saldi,
                suggestion(saldi), positions);
    }

    public void markSettled(YearMonth month) {
        if (!settledMonthRepository.existsByMonat(month.toString())) {
            settledMonthRepository.save(new SettledMonthEntity(month.toString(), true));
        }
    }

    public List<SettlementMonth> history(int months) {
        YearMonth currentMonth = YearMonth.now(clock);
        List<SettlementMonth> history = new ArrayList<>();
        for (int back = months - 1; back >= 0; back--) {
            SettlementWithPositions settlement = settlement(currentMonth.minusMonths(back));
            history.add(new SettlementMonth(settlement.monat(), settlement.laeuftNoch(),
                    settlement.ausgeglichen(), settlement.saldi(), settlement.vorschlag()));
        }
        return history;
    }

    private Map<String, Integer> percentagesFor(String category, List<String> persons) {
        KategorieEntity configured = categoryRepository.findById(category).orElse(null);
        if (configured != null && "ANTEILE".equals(configured.getArt())
                && !configured.getAnteile().isEmpty()) {
            return configured.getAnteile();
        }
        Map<String, Integer> halfHalf = new LinkedHashMap<>();
        int percent = persons.isEmpty() ? 0 : 100 / persons.size();
        for (String person : persons) {
            halfHalf.put(person, percent);
        }
        return halfHalf;
    }

    private Map<String, BigDecimal> computeShares(BigDecimal total, Map<String, Integer> percentages) {
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal distributed = BigDecimal.ZERO.setScale(2);
        Map.Entry<String, Integer> largest = null;
        for (Map.Entry<String, Integer> percent : percentages.entrySet()) {
            BigDecimal share = total.multiply(BigDecimal.valueOf(percent.getValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            shares.put(percent.getKey(), share);
            distributed = distributed.add(share);
            if (largest == null || percent.getValue() > largest.getValue()) {
                largest = percent;
            }
        }
        BigDecimal remainder = total.subtract(distributed);
        if (remainder.signum() != 0 && largest != null) {
            shares.put(largest.getKey(), shares.get(largest.getKey()).add(remainder));
        }
        return shares;
    }

    private String suggestion(Map<String, BigDecimal> saldi) {
        Map.Entry<String, BigDecimal> largest = null;
        Map.Entry<String, BigDecimal> smallest = null;
        for (Map.Entry<String, BigDecimal> saldo : saldi.entrySet()) {
            if (largest == null || saldo.getValue().compareTo(largest.getValue()) > 0) {
                largest = saldo;
            }
            if (smallest == null || saldo.getValue().compareTo(smallest.getValue()) < 0) {
                smallest = saldo;
            }
        }
        if (largest == null || largest.getValue().signum() <= 0) {
            return null;
        }
        return smallest.getKey() + " schuldet " + largest.getKey() + " "
                + asText(largest.getValue()) + " €";
    }

    private String asText(BigDecimal amount) {
        return new java.text.DecimalFormat("#,##0.00",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.GERMANY))
                .format(amount);
    }
}
