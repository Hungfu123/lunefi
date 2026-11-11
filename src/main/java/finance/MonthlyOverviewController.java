package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
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
    private final AuthenticatedUser authenticatedUser;

    public MonthlyOverviewController(EntryRepository entryRepository, BudgetRepository budgetRepository,
            AuthenticatedUser authenticatedUser) {
        this.entryRepository = entryRepository;
        this.budgetRepository = budgetRepository;
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

        List<Entry> inMonth = entryRepository.findAll().stream()
                .map(EntryEntity::asEntry)
                .filter(entry -> YearMonth.from(entry.datum()).equals(period))
                .filter(entry -> person == null || person.equals(entry.person()))
                .filter(entry -> !Boolean.TRUE.equals(gemeinsam) || Boolean.TRUE.equals(entry.gemeinsam()))
                .sorted(Comparator.comparing(Entry::datum))
                .toList();

        Map<String, BigDecimal> expenses = new HashMap<>();
        Map<String, BigDecimal> income = new HashMap<>();
        for (Entry entry : inMonth) {
            Map<String, BigDecimal> target = entry.betrag().signum() < 0 ? expenses : income;
            target.merge(entry.kategorie(), entry.betrag().abs(), BigDecimal::add);
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

        return new MonthlyOverview(period.toString(), expenses, income, inMonth, budgets);
    }
}
