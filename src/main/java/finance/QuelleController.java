package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quellen")
public class QuelleController {

    private final QuelleRepository quellen;
    private final KontoRepository konten;
    private final EintragRepository eintraege;
    private final LernregelRepository lernregeln;

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY);

    public QuelleController(QuelleRepository quellen, KontoRepository konten,
            EintragRepository eintraege, LernregelRepository lernregeln) {
        this.quellen = quellen;
        this.konten = konten;
        this.eintraege = eintraege;
        this.lernregeln = lernregeln;
    }

    @PostMapping
    public ResponseEntity<Quelle> anlegen(@RequestBody QuelleAnlegen daten) {
        if (daten.name() == null || daten.name().isBlank()
                || daten.art() == null || daten.art().isBlank()
                || daten.konto() == null || daten.konto().isBlank()
                || !konten.existsByName(daten.konto())
                || quellen.existsByName(daten.name())) {
            return ResponseEntity.badRequest().build();
        }
        QuelleEntity gespeichert = quellen.save(new QuelleEntity(daten.name(), daten.art(), daten.konto()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Quelle(gespeichert.getId(), gespeichert.getName(), gespeichert.getArt(),
                        gespeichert.getKonto(), gespeichert.getZustand()));
    }

    @GetMapping
    public List<Quelle> alle() {
        List<Quelle> ergebnis = new ArrayList<>();
        for (QuelleEntity quelle : quellen.findAll()) {
            ergebnis.add(new Quelle(quelle.getId(), quelle.getName(), quelle.getArt(),
                    quelle.getKonto(), quelle.getZustand()));
        }
        return ergebnis;
    }

    @PostMapping("/{id}/import")
    public ResponseEntity<ImportErgebnis> importieren(@PathVariable Long id, @RequestBody ImportAnfrage anfrage) {
        QuelleEntity quelle = quellen.findById(id).orElse(null);
        if (quelle == null) {
            return ResponseEntity.notFound().build();
        }
        KontoEntity konto = konten.findByName(quelle.getKonto()).orElse(null);
        if (konto == null) {
            return ResponseEntity.badRequest().build();
        }

        int neu = 0;
        int uebersprungen = 0;
        for (UmsatzDto umsatz : anfrage.umsaetze()) {
            if (eintraege.existsByQuelleIdAndExterneKennung(id, umsatz.externeKennung())) {
                uebersprungen++;
                continue;
            }
            EintragEntity eintrag = new EintragEntity(
                    LocalDate.parse(umsatz.datum(), DATUM),
                    betragAus(umsatz.betrag()),
                    kategorieFuer(umsatz.verwendungszweck()),
                    konto.getPerson(),
                    false,
                    null,
                    konto.getName());
            eintrag.setQuelleId(id);
            eintrag.setExterneKennung(umsatz.externeKennung());
            eintrag.setVerwendungszweck(umsatz.verwendungszweck());
            eintraege.save(eintrag);
            neu++;
        }
        return ResponseEntity.ok(new ImportErgebnis(neu, uebersprungen));
    }

    private String kategorieFuer(String verwendungszweck) {
        if (verwendungszweck != null) {
            String gross = verwendungszweck.toUpperCase(Locale.GERMANY);
            for (LernregelEntity regel : lernregeln.findAll()) {
                if (gross.contains(regel.getKennung().toUpperCase(Locale.GERMANY))) {
                    return regel.getKategorie();
                }
            }
        }
        return "Import";
    }

    private BigDecimal betragAus(String text) {
        String normalisiert = text.replace(".", "").replace(',', '.');
        if (normalisiert.startsWith("+")) {
            normalisiert = normalisiert.substring(1);
        }
        return new BigDecimal(normalisiert);
    }

    public record QuelleAnlegen(String name, String art, String konto) {
    }

    public record UmsatzDto(String externeKennung, String datum, String betrag, String verwendungszweck) {
    }

    public record ImportAnfrage(List<UmsatzDto> umsaetze) {
    }

    public record ImportErgebnis(int neu, int uebersprungen) {
    }
}
