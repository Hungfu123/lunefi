package finance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/konten")
public class KontoController {

    private final KontoRepository konten;
    private final EintragRepository eintraege;
    private final PersonRepository personen;
    private final AngemeldetePerson angemeldetePerson;

    public KontoController(KontoRepository konten, EintragRepository eintraege,
            PersonRepository personen, AngemeldetePerson angemeldetePerson) {
        this.konten = konten;
        this.eintraege = eintraege;
        this.personen = personen;
        this.angemeldetePerson = angemeldetePerson;
    }

    @PostMapping
    public ResponseEntity<Konto> create(@RequestBody Konto konto) {
        if (konto.person() != null && !konto.person().isBlank()) {
            angemeldetePerson.pruefeEigenePerson(konto.person());
        }
        if (konto.name() == null || konto.name().isBlank()
                || konto.person() == null || konto.person().isBlank()
                || konto.startSaldo() == null
                || !personen.existsByName(konto.person())
                || konten.existsByName(konto.name())) {
            return ResponseEntity.badRequest().build();
        }
        KontoEntity gespeichert = konten.save(new KontoEntity(konto.name(), konto.person(), konto.startSaldo()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Konto(gespeichert.getId(), gespeichert.getName(), gespeichert.getPerson(),
                        gespeichert.getStartSaldo(), gespeichert.getStartSaldo()));
    }

    @GetMapping
    public List<Konto> list(@RequestParam(required = false) String person) {
        if (person != null) {
            angemeldetePerson.pruefeEigenePerson(person);
        }
        List<Konto> ergebnis = new ArrayList<>();
        for (KontoEntity konto : konten.findAll()) {
            if (person != null && !person.equals(konto.getPerson())) {
                continue;
            }
            ergebnis.add(new Konto(konto.getId(), konto.getName(), konto.getPerson(),
                    konto.getStartSaldo(), currentBalance(konto)));
        }
        return ergebnis;
    }

    private BigDecimal currentBalance(KontoEntity konto) {
        BigDecimal summe = konto.getStartSaldo();
        for (EintragEntity eintrag : eintraege.findAll()) {
            if (konto.getName().equals(eintrag.getKonto())) {
                summe = summe.add(eintrag.getBetrag());
            }
        }
        return summe;
    }
}
