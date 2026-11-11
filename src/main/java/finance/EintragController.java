package finance;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eintraege")
public class EintragController {

    private final EintragRepository repository;
    private final PersonRepository personen;
    private final KontoRepository konten;
    private final AngemeldetePerson angemeldetePerson;

    public EintragController(EintragRepository repository, PersonRepository personen,
            KontoRepository konten, AngemeldetePerson angemeldetePerson) {
        this.repository = repository;
        this.personen = personen;
        this.konten = konten;
        this.angemeldetePerson = angemeldetePerson;
    }

    @PostMapping
    public ResponseEntity<Eintrag> capture(@RequestBody Eintrag eintrag) {
        if (!istEigeneOderGemeinsam(eintrag)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isValid(eintrag)) {
            return ResponseEntity.badRequest().build();
        }
        EintragEntity gespeichert = repository.save(new EintragEntity(
                eintrag.datum(), eintrag.betrag(), eintrag.kategorie(),
                eintrag.person(), Boolean.TRUE.equals(eintrag.gemeinsam()), eintrag.zahler(),
                eintrag.konto()));
        return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.asEntry());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Eintrag> update(@PathVariable long id, @RequestBody Eintrag eintrag) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!istEigeneOderGemeinsam(eintrag)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        EintragEntity vorhandener = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(vorhandener.getGemeinsam())) {
            angemeldetePerson.pruefeEigenePerson(vorhandener.getPerson());
        }
        if (!isValid(eintrag)) {
            return ResponseEntity.badRequest().build();
        }
        vorhandener.update(eintrag.datum(), eintrag.betrag(), eintrag.kategorie(),
                eintrag.person(), Boolean.TRUE.equals(eintrag.gemeinsam()), eintrag.zahler(),
                eintrag.konto());
        return ResponseEntity.ok(repository.save(vorhandener).asEntry());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        EintragEntity vorhandener = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(vorhandener.getGemeinsam())) {
            angemeldetePerson.pruefeEigenePerson(vorhandener.getPerson());
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Eintrag> list(@RequestParam(required = false) String monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (person != null) {
            angemeldetePerson.pruefeEigenePerson(person);
        }
        YearMonth zeitraum = monat == null ? null : monthAsYearMonth(monat);
        return repository.findAll().stream()
                .map(EintragEntity::asEntry)
                .filter(eintrag -> zeitraum == null || YearMonth.from(eintrag.datum()).equals(zeitraum))
                .filter(eintrag -> person == null || person.equals(eintrag.person()))
                .filter(eintrag -> gemeinsam == null
                        || gemeinsam.equals(Boolean.TRUE.equals(eintrag.gemeinsam())))
                .toList();
    }

    private boolean istEigeneOderGemeinsam(Eintrag eintrag) {
        boolean persoenlich = eintrag.person() != null && !eintrag.person().isBlank();
        return !persoenlich || Objects.equals(angemeldetePerson.name(), eintrag.person());
    }

    private boolean isValid(Eintrag eintrag) {
        return eintrag.kategorie() != null && !eintrag.kategorie().isBlank()
                && eintrag.datum() != null
                && eintrag.betrag() != null
                && exactlyOneAssignment(eintrag)
                && payerIfShared(eintrag)
                && (eintrag.konto() == null || konten.existsByName(eintrag.konto()));
    }

    private boolean exactlyOneAssignment(Eintrag eintrag) {
        boolean personGesetzt = eintrag.person() != null && !eintrag.person().isBlank();
        return personGesetzt ^ Boolean.TRUE.equals(eintrag.gemeinsam());
    }

    private boolean payerIfShared(Eintrag eintrag) {
        if (!Boolean.TRUE.equals(eintrag.gemeinsam())) {
            return true;
        }
        return eintrag.zahler() != null && !eintrag.zahler().isBlank()
                && personen.existsByName(eintrag.zahler());
    }

    private YearMonth monthAsYearMonth(String monat) {
        try {
            return YearMonth.parse(monat);
        } catch (java.time.format.DateTimeParseException ausnahme) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
