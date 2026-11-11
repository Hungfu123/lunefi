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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monatsuebersicht")
public class MonatsuebersichtController {

    private final EintragRepository eintragRepository;
    private final BudgetRepository budgetRepository;

    public MonatsuebersichtController(EintragRepository eintragRepository, BudgetRepository budgetRepository) {
        this.eintragRepository = eintragRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/{jahr}-{monat}")
    public Monatsuebersicht forMonth(@PathVariable int jahr, @PathVariable int monat) {
        YearMonth zeitraum;
        try {
            zeitraum = YearMonth.of(jahr, monat);
        } catch (java.time.DateTimeException ausnahme) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        List<Eintrag> imMonat = eintragRepository.findAll().stream()
                .map(EintragEntity::asEntry)
                .filter(eintrag -> YearMonth.from(eintrag.datum()).equals(zeitraum))
                .sorted(Comparator.comparing(Eintrag::datum))
                .toList();

        Map<String, BigDecimal> ausgaben = new HashMap<>();
        Map<String, BigDecimal> einnahmen = new HashMap<>();
        for (Eintrag eintrag : imMonat) {
            Map<String, BigDecimal> ziel = eintrag.betrag().signum() < 0 ? ausgaben : einnahmen;
            ziel.merge(eintrag.kategorie(), eintrag.betrag().abs(), BigDecimal::add);
        }

        Map<String, BudgetInfo> budgets = new LinkedHashMap<>();
        for (BudgetEntity budget : budgetRepository.findAll()) {
            if (!budget.getMonat().equals(zeitraum.toString())) {
                continue;
            }
            BigDecimal ist = ausgaben.getOrDefault(budget.getKategorie(), BigDecimal.ZERO);
            budgets.put(budget.getKategorie(),
                    new BudgetInfo(budget.getLimit(), ist, ist.compareTo(budget.getLimit()) > 0));
        }

        return new Monatsuebersicht(zeitraum.toString(), ausgaben, einnahmen, imMonat, budgets);
    }
}
