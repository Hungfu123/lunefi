package finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sparziele")
public class SavingsGoalController {

    private final SavingsGoalRepository repository;

    public SavingsGoalController(SavingsGoalRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<SavingsGoal> create(@RequestBody SavingsGoal goal) {
        if (goal.name() == null || goal.name().isBlank()
                || goal.zielbetrag() == null
                || goal.zielbetrag().signum() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        SavingsGoalEntity saved = repository.save(
                new SavingsGoalEntity(goal.name(), goal.zielbetrag()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.asSavingsGoal());
    }

    @GetMapping
    public List<SavingsGoal> list() {
        return repository.findAll().stream().map(SavingsGoalEntity::asSavingsGoal).toList();
    }

    @PostMapping("/{id}/einzahlungen")
    public ResponseEntity<SavingsGoal> deposit(@PathVariable Long id, @RequestBody Deposit deposit) {
        SavingsGoalEntity goal = repository.findById(id).orElse(null);
        if (goal == null) {
            return ResponseEntity.notFound().build();
        }
        if (deposit.betrag() == null || deposit.betrag().signum() <= 0
                || deposit.person() == null || deposit.person().isBlank()
                || deposit.datum() == null) {
            return ResponseEntity.badRequest().build();
        }
        goal.getEinzahlungen().add(new DepositEntity(
                deposit.betrag(), deposit.person(), deposit.datum()));
        SavingsGoalEntity saved = repository.save(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.asSavingsGoal());
    }
}
