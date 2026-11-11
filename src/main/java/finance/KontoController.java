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

    public KontoController(KontoRepository konten, EintragRepository eintraege, PersonRepository personen) {
        this.konten = konten;
        this.eintraege = eintraege;
        this.personen = personen;
    }

    @PostMapping
    public ResponseEntity<Konto> anlegen(@RequestBody Konto konto) {
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
    public List<Konto> alle(@RequestParam(required = false) String person) {
        List<Konto> ergebnis = new ArrayList<>();
        for (KontoEntity konto : konten.findAll()) {
            if (person != null && !person.equals(konto.getPerson())) {
                continue;
            }
            ergebnis.add(new Konto(konto.getId(), konto.getName(), konto.getPerson(),
                    konto.getStartSaldo(), saldo(konto)));
        }
        return ergebnis;
    }

    private BigDecimal saldo(KontoEntity konto) {
        BigDecimal summe = konto.getStartSaldo();
        for (EintragEntity eintrag : eintraege.findAll()) {
            if (konto.getName().equals(eintrag.getKonto())) {
                summe = summe.add(eintrag.getBetrag());
            }
        }
        return summe;
    }
}
