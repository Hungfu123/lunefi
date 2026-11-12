package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monatsuebersicht")
public class MonthlyOverviewController {

    private final EntryRepository entryRepository;
    private final BudgetRepository budgetRepository;
    private final EntryParts entryParts;
    private final AuthenticatedUser authenticatedUser;

    public MonthlyOverviewController(EntryRepository entryRepository, BudgetRepository budgetRepository,
            EntryParts entryParts, AuthenticatedUser authenticatedUser) {
        this.entryRepository = entryRepository;
        this.budgetRepository = budgetRepository;
        this.entryParts = entryParts;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/{jahr}-{monat}")
    public MonthlyOverview forMonth(@PathVariable int jahr, @PathVariable int monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        YearMonth period;
        try {
            period = YearMonth.of(jahr, monat);
        } catch (java.time.DateTimeException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (person != null && gemeinsam != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }

        List<EntryEntity> entities = entryRepository.findAll().stream()
                .filter(entry -> YearMonth.from(entry.getDatum()).equals(period))
                .sorted(Comparator.comparing(EntryEntity::getDatum)
                        .thenComparing(EntryEntity::getId))
                .toList();
        List<LineItem> items = entryParts.lineItems(entities);

        List<LineItem> visible = items.stream()
                .filter(item -> person == null || person.equals(item.person()))
                .filter(item -> !Boolean.TRUE.equals(gemeinsam) || item.shared())
                .toList();

        Map<String, BigDecimal> expenses = new HashMap<>();
        Map<String, BigDecimal> income = new HashMap<>();
        for (LineItem item : visible) {
            Map<String, BigDecimal> target = item.amount().signum() < 0 ? expenses : income;
            target.merge(item.category(), item.amount().abs(), BigDecimal::add);
        }

        Map<String, BudgetInfo> budgets = new LinkedHashMap<>();
        for (BudgetEntity budget : budgetRepository.findAll()) {
            if (!budget.getMonat().equals(period.toString())) {
                continue;
            }
            BigDecimal actual = expenses.getOrDefault(budget.getKategorie(), BigDecimal.ZERO);
            budgets.put(budget.getKategorie(),
                    new BudgetInfo(budget.getLimit(), actual, actual.compareTo(budget.getLimit()) > 0));
        }

        return new MonthlyOverview(period.toString(), expenses, income,
                visibleEntries(entities, items, person, gemeinsam), budgets);
    }

    private List<Entry> visibleEntries(List<EntryEntity> entities, List<LineItem> items,
            String person, Boolean gemeinsam) {
        boolean filtered = person != null || gemeinsam != null;
        if (!filtered) {
            return entities.stream().map(entryParts::toApi).toList();
        }
        Map<Long, List<LineItem>> byEntry = new LinkedHashMap<>();
        for (LineItem item : items) {
            boolean matches = (person == null || person.equals(item.person()))
                    && (!Boolean.TRUE.equals(gemeinsam) || item.shared());
            if (matches) {
                byEntry.computeIfAbsent(item.entryId(), id -> new ArrayList<>()).add(item);
            }
        }
        List<Entry> result = new ArrayList<>();
        for (EntryEntity entity : entities) {
            List<LineItem> matching = byEntry.get(entity.getId());
            if (matching == null) {
                continue;
            }
            BigDecimal visibleAmount = BigDecimal.ZERO;
            for (LineItem item : matching) {
                visibleAmount = visibleAmount.add(item.amount());
            }
            List<EntryPart> parts = matching.stream()
                    .filter(item -> item.partId() != null)
                    .map(item -> new EntryPart(item.amount(), item.category(), item.person(), item.shared()))
                    .toList();
            result.add(new Entry(entity.getId(), entity.getDatum(), visibleAmount,
                    primaryCategory(matching), person,
                    Boolean.TRUE.equals(gemeinsam),
                    entity.getZahler(), entity.getKonto(), entity.getVerwendungszweck(),
                    entity.getQuelleId(), parts.isEmpty() ? null : parts));
        }
        return result;
    }

    private String primaryCategory(List<LineItem> matching) {
        LineItem primary = null;
        for (LineItem item : matching) {
            if (primary == null || item.amount().abs().compareTo(primary.amount().abs()) > 0) {
                primary = item;
            }
        }
        return primary == null ? null : primary.category();
    }
}
