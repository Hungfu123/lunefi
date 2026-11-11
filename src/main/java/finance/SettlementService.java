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
    private final PersonRepository personRepository;
    private final CategoryRepository categoryRepository;
    private final SettledMonthRepository settledMonthRepository;
    private final java.time.Clock clock;

    public SettlementService(EntryRepository entryRepository, PersonRepository personRepository,
            CategoryRepository categoryRepository, SettledMonthRepository settledMonthRepository,
            java.time.Clock clock) {
        this.entryRepository = entryRepository;
        this.personRepository = personRepository;
        this.categoryRepository = categoryRepository;
        this.settledMonthRepository = settledMonthRepository;
        this.clock = clock;
    }

    public SettlementWithPositions settlement(YearMonth monat) {
        List<String> persons = personRepository.findAll().stream()
                .map(PersonEntity::getName)
                .sorted()
                .toList();

        Map<String, BigDecimal> saldi = new LinkedHashMap<>();
        for (String person : persons) {
            saldi.put(person, BigDecimal.ZERO.setScale(2));
        }

        List<EntryEntity> sharedEntries = entryRepository.findAll().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getGemeinsam()))
                .filter(entry -> YearMonth.from(entry.getDatum()).equals(monat))
                .sorted(Comparator.comparing(EntryEntity::getDatum)
                        .thenComparing(EntryEntity::getId))
                .toList();

        List<SettlementPosition> positions = new ArrayList<>();
        for (EntryEntity entry : sharedEntries) {
            Map<String, BigDecimal> shares = sharesFor(entry, persons);
            positions.add(new SettlementPosition(entry.getId(), entry.getKategorie(),
                    entry.getBetrag(), entry.getZahler(), shares));

            BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            int sign = entry.getBetrag().signum() < 0 ? 1 : -1;
            for (Map.Entry<String, BigDecimal> share : shares.entrySet()) {
                BigDecimal advancedForOthers = share.getKey().equals(entry.getZahler())
                        ? total.subtract(share.getValue())
                        : share.getValue().negate();
                saldi.merge(share.getKey(), advancedForOthers.multiply(
                        BigDecimal.valueOf(sign)), BigDecimal::add);
            }
        }

        boolean stillRunning = monat.equals(YearMonth.now(clock));
        return new SettlementWithPositions(monat.toString(), stillRunning,
                settledMonthRepository.existsByMonat(monat.toString()), saldi,
                suggestion(saldi), positions);
    }

    public void markSettled(YearMonth monat) {
        if (!settledMonthRepository.existsByMonat(monat.toString())) {
            settledMonthRepository.save(new SettledMonthEntity(monat.toString(), true));
        }
    }

    public List<SettlementMonth> history(int monate) {
        YearMonth currentMonth = YearMonth.now(clock);
        List<SettlementMonth> history = new ArrayList<>();
        for (int back = monate - 1; back >= 0; back--) {
            SettlementWithPositions settlement = settlement(currentMonth.minusMonths(back));
            history.add(new SettlementMonth(settlement.monat(), settlement.laeuftNoch(),
                    settlement.ausgeglichen(), settlement.saldi(), settlement.vorschlag()));
        }
        return history;
    }

    private Map<String, BigDecimal> sharesFor(EntryEntity entry, List<String> persons) {
        BigDecimal total = entry.getBetrag().abs().setScale(2, RoundingMode.HALF_UP);
        Map<String, Integer> percentages = percentagesFor(entry.getKategorie(), persons);
        return computeShares(total, percentages);
    }

    private Map<String, Integer> percentagesFor(String kategorie, List<String> persons) {
        KategorieEntity configured = categoryRepository.findById(kategorie).orElse(null);
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

    private String asText(BigDecimal betrag) {
        return new java.text.DecimalFormat("#,##0.00",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.GERMANY))
                .format(betrag);
    }
}
