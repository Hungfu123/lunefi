package finance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lernregeln")
public class LearningRuleController {

    private final LearningRuleRepository learningRuleRepository;

    public LearningRuleController(LearningRuleRepository learningRuleRepository) {
        this.learningRuleRepository = learningRuleRepository;
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody LearningRuleInput input) {
        if (input.kennung() == null || input.kennung().isBlank()
                || input.kategorie() == null || input.kategorie().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        learningRuleRepository.save(new LearningRuleEntity(input.kennung(), input.kategorie()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record LearningRuleInput(String kennung, String kategorie) {
    }
}
