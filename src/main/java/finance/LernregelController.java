package finance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lernregeln")
public class LernregelController {

    private final LernregelRepository lernregeln;

    public LernregelController(LernregelRepository lernregeln) {
        this.lernregeln = lernregeln;
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody LernregelAnlegen daten) {
        if (daten.kennung() == null || daten.kennung().isBlank()
                || daten.kategorie() == null || daten.kategorie().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        lernregeln.save(new LernregelEntity(daten.kennung(), daten.kategorie()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record LernregelAnlegen(String kennung, String kategorie) {
    }
}
