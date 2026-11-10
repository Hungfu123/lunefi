package finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eintraege")
public class EintragController {

    private final EintragSpeicher speicher;

    public EintragController(EintragSpeicher speicher) {
        this.speicher = speicher;
    }

    @PostMapping
    public ResponseEntity<Eintrag> erfassen(@RequestBody Eintrag eintrag) {
        if (eintrag.kategorie() == null || eintrag.kategorie().isBlank()
                || eintrag.datum() == null
                || eintrag.betrag() == null) {
            return ResponseEntity.badRequest().build();
        }
        speicher.hinzufuegen(eintrag);
        return ResponseEntity.status(HttpStatus.CREATED).body(eintrag);
    }

    @GetMapping
    public List<Eintrag> alle() {
        return speicher.alle();
    }
}
