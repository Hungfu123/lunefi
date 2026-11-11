package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coaching")
public class CoachingController {

    private final BudgetRepository budgetRepository;
    private final EntryRepository entryRepository;
    private final CategoryRepository categoryRepository;

    public CoachingController(BudgetRepository budgetRepository, EntryRepository entryRepository,
            CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<CoachingResponse> coaching(@RequestParam String monat) {
        YearMonth period;
        try {
            period = YearMonth.parse(monat);
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, BigDecimal> expenses = new HashMap<>();
        for (EntryEntity entry : entryRepository.findAll()) {
            if (!YearMonth.from(entry.getDatum()).equals(period)) {
                continue;
            }
            BigDecimal amount = entry.getBetrag();
            if (amount.signum() < 0) {
                expenses.merge(entry.getKategorie(), amount.abs(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> remainingBudgets = new TreeMap<>();
        List<CoachingRecommendation> recommendations = new ArrayList<>();
        List<CoachingSaved> wellSaved = new ArrayList<>();

        budgetRepository.findAll().stream()
                .filter(budget -> budget.getMonat().equals(period.toString()))
                .sorted(Comparator.comparing(BudgetEntity::getKategorie))
                .forEach(budget -> evaluate(budget, expenses, remainingBudgets, recommendations, wellSaved));

        return ResponseEntity.ok(
                new CoachingResponse(period.toString(), remainingBudgets, recommendations, wellSaved));
    }

    private void evaluate(BudgetEntity budget, Map<String, BigDecimal> expenses,
            Map<String, BigDecimal> remainingBudgets, List<CoachingRecommendation> recommendations,
            List<CoachingSaved> wellSaved) {
        String category = budget.getKategorie();
        BigDecimal actual = expenses.getOrDefault(category, BigDecimal.ZERO);
        BigDecimal headroom = budget.getLimit().subtract(actual);
        if (headroom.signum() < 0) {
            return;
        }
        String attitude = categoryRepository.findById(category)
                .map(KategorieEntity::asAttitude)
                .orElse(Attitude.FLEXIBEL);
        if (Attitude.BEWUSST_MEHR.equals(attitude)) {
            if (headroom.signum() > 0) {
                recommendations.add(new CoachingRecommendation(category, attitude, actual, budget.getLimit(),
                        headroom, tipText(category, headroom)));
            }
        } else if (Attitude.SPAREN.equals(attitude)) {
            wellSaved.add(new CoachingSaved(category, attitude, actual, budget.getLimit(), headroom));
        } else {
            remainingBudgets.put(category, headroom);
        }
    }

    private String tipText(String category, BigDecimal headroom) {
        return category + " verträgt " + String.format(Locale.GERMANY, "%.2f", headroom)
                + " € mehr — bewusst genießen";
    }
}
