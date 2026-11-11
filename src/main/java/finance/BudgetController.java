package finance;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BudgetController {

    private final BudgetRepository repository;

    public BudgetController(BudgetRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/api/budgets")
    public ResponseEntity<Budget> set(@RequestBody Budget budget) {
        if (budget.monat() == null || budget.kategorie() == null || budget.kategorie().isBlank()
                || budget.limit() == null || budget.limit().signum() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            YearMonth.parse(budget.monat());
        } catch (DateTimeParseException ausnahme) {
            return ResponseEntity.badRequest().build();
        }
        BudgetEntity gespeichert = repository.findByMonatAndKategorie(budget.monat(), budget.kategorie())
                .map(vorhandenes -> {
                    vorhandenes.setLimit(budget.limit());
                    return vorhandenes;
                })
                .orElseGet(() -> new BudgetEntity(budget.monat(), budget.kategorie(), budget.limit()));
        return ResponseEntity.ok(repository.save(gespeichert).asBudget());
    }
}
