package finance;

import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    public EintragController(EintragRepository repository, PersonRepository personen, KontoRepository konten) {
        this.repository = repository;
        this.personen = personen;
        this.konten = konten;
    }

    @PostMapping
    public ResponseEntity<Eintrag> erfassen(@RequestBody Eintrag eintrag) {
        if (eintrag.kategorie() == null || eintrag.kategorie().isBlank()
                || eintrag.datum() == null
                || eintrag.betrag() == null
                || !genauEineZuordnung(eintrag)
                || !zahlerBeiGemeinsam(eintrag)
                || (eintrag.konto() != null && !konten.existsByName(eintrag.konto()))) {
            return ResponseEntity.badRequest().build();
        }
        EintragEntity gespeichert = repository.save(new EintragEntity(
                eintrag.datum(), eintrag.betrag(), eintrag.kategorie(),
                eintrag.person(), Boolean.TRUE.equals(eintrag.gemeinsam()), eintrag.zahler(),
                eintrag.konto()));
        return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.alsEintrag());
    }

    @GetMapping
    public List<Eintrag> alle(@RequestParam(required = false) String monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        YearMonth zeitraum = monat == null ? null : monatAlsJahrMonat(monat);
        return repository.findAll().stream()
                .map(EintragEntity::alsEintrag)
                .filter(eintrag -> zeitraum == null || YearMonth.from(eintrag.datum()).equals(zeitraum))
                .filter(eintrag -> person == null || person.equals(eintrag.person()))
                .filter(eintrag -> gemeinsam == null
                        || gemeinsam.equals(Boolean.TRUE.equals(eintrag.gemeinsam())))
                .toList();
    }

    private boolean genauEineZuordnung(Eintrag eintrag) {
        boolean personGesetzt = eintrag.person() != null && !eintrag.person().isBlank();
        return personGesetzt ^ Boolean.TRUE.equals(eintrag.gemeinsam());
    }

    private boolean zahlerBeiGemeinsam(Eintrag eintrag) {
        if (!Boolean.TRUE.equals(eintrag.gemeinsam())) {
            return true;
        }
        return eintrag.zahler() != null && !eintrag.zahler().isBlank()
                && personen.existsByName(eintrag.zahler());
    }

    private YearMonth monatAlsJahrMonat(String monat) {
        try {
            return YearMonth.parse(monat);
        } catch (java.time.format.DateTimeParseException ausnahme) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
