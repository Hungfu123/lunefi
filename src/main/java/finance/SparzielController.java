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
public class SparzielController {

    private final SparzielRepository repository;

    public SparzielController(SparzielRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Sparziel> anlegen(@RequestBody Sparziel sparziel) {
        if (sparziel.name() == null || sparziel.name().isBlank()
                || sparziel.zielbetrag() == null
                || sparziel.zielbetrag().signum() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        SparzielEntity gespeichert = repository.save(
                new SparzielEntity(sparziel.name(), sparziel.zielbetrag()));
        return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.alsSparziel());
    }

    @GetMapping
    public List<Sparziel> alle() {
        return repository.findAll().stream().map(SparzielEntity::alsSparziel).toList();
    }

    @PostMapping("/{id}/einzahlungen")
    public ResponseEntity<Sparziel> einzahlen(@PathVariable Long id, @RequestBody Einzahlung einzahlung) {
        SparzielEntity sparziel = repository.findById(id).orElse(null);
        if (sparziel == null) {
            return ResponseEntity.notFound().build();
        }
        if (einzahlung.betrag() == null || einzahlung.betrag().signum() <= 0
                || einzahlung.person() == null || einzahlung.person().isBlank()
                || einzahlung.datum() == null) {
            return ResponseEntity.badRequest().build();
        }
        sparziel.getEinzahlungen().add(new EinzahlungEntity(
                einzahlung.betrag(), einzahlung.person(), einzahlung.datum()));
        SparzielEntity gespeichert = repository.save(sparziel);
        return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.alsSparziel());
    }
}
