package finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kategorien")
public class KategorieController {

    private final KategorieRepository kategorieRepository;
    private final EintragRepository eintragRepository;
    private final PersonRepository personen;

    public KategorieController(KategorieRepository kategorieRepository, EintragRepository eintragRepository,
            PersonRepository personen) {
        this.kategorieRepository = kategorieRepository;
        this.eintragRepository = eintragRepository;
        this.personen = personen;
    }

    @GetMapping
    public List<KategorieInfo> alle() {
        TreeSet<String> namen = new TreeSet<>();
        eintragRepository.findAll().forEach(eintrag -> namen.add(eintrag.getKategorie()));
        kategorieRepository.findAll().forEach(kategorie -> namen.add(kategorie.getName()));

        List<KategorieInfo> kategorien = new ArrayList<>();
        for (String name : namen) {
            KategorieEntity konfiguriert = kategorieRepository.findById(name).orElse(null);
            SplitKonfiguration split = konfiguriert == null
                    ? new SplitKonfiguration("HALBHALB", null)
                    : konfiguriert.alsSplitKonfiguration();
            kategorien.add(new KategorieInfo(name, split));
        }
        return kategorien;
    }

    @PutMapping("/{kategorie}/split")
    public ResponseEntity<SplitKonfiguration> splitSetzen(@PathVariable String kategorie,
            @RequestBody SplitKonfiguration split) {
        if (!gueltig(split)) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Integer> anteile = "ANTEILE".equals(split.art()) ? split.anteile() : Map.of();
        KategorieEntity gespeichert = kategorieRepository.save(
                new KategorieEntity(kategorie, split.art(), anteile));
        return ResponseEntity.ok(gespeichert.alsSplitKonfiguration());
    }

    private boolean gueltig(SplitKonfiguration split) {
        if (split == null || !("HALBHALB".equals(split.art()) || "ANTEILE".equals(split.art()))) {
            return false;
        }
        if (!"ANTEILE".equals(split.art())) {
            return true;
        }
        Map<String, Integer> anteile = split.anteile();
        if (anteile == null || anteile.isEmpty()) {
            return false;
        }
        int summe = 0;
        for (Map.Entry<String, Integer> eintrag : anteile.entrySet()) {
            if (eintrag.getKey() == null || eintrag.getKey().isBlank()
                    || eintrag.getValue() == null || eintrag.getValue() <= 0
                    || !personen.existsByName(eintrag.getKey())) {
                return false;
            }
            summe += eintrag.getValue();
        }
        return summe == 100;
    }
}
