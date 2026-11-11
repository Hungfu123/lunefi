package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coaching")
public class CoachingController {

    private final BudgetRepository budgetRepository;
    private final EintragRepository eintragRepository;
    private final KategorieRepository kategorieRepository;

    public CoachingController(BudgetRepository budgetRepository, EintragRepository eintragRepository,
            KategorieRepository kategorieRepository) {
        this.budgetRepository = budgetRepository;
        this.eintragRepository = eintragRepository;
        this.kategorieRepository = kategorieRepository;
    }

    @GetMapping
    public ResponseEntity<CoachingAntwort> coaching(@RequestParam String monat) {
        YearMonth zeitraum;
        try {
            zeitraum = YearMonth.parse(monat);
        } catch (DateTimeParseException ausnahme) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, BigDecimal> ausgaben = new HashMap<>();
        for (EintragEntity eintrag : eintragRepository.findAll()) {
            if (!YearMonth.from(eintrag.getDatum()).equals(zeitraum)) {
                continue;
            }
            BigDecimal betrag = eintrag.getBetrag();
            if (betrag.signum() < 0) {
                ausgaben.merge(eintrag.getKategorie(), betrag.abs(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> budgetsUebrig = new TreeMap<>();
        List<CoachingEmpfehlung> empfehlungen = new ArrayList<>();
        List<CoachingErspart> gutGespart = new ArrayList<>();

        budgetRepository.findAll().stream()
                .filter(budget -> budget.getMonat().equals(zeitraum.toString()))
                .sorted(Comparator.comparing(BudgetEntity::getKategorie))
                .forEach(budget -> bewerte(budget, ausgaben, budgetsUebrig, empfehlungen, gutGespart));

        return ResponseEntity.ok(
                new CoachingAntwort(zeitraum.toString(), budgetsUebrig, empfehlungen, gutGespart));
    }

    private void bewerte(BudgetEntity budget, Map<String, BigDecimal> ausgaben,
            Map<String, BigDecimal> budgetsUebrig, List<CoachingEmpfehlung> empfehlungen,
            List<CoachingErspart> gutGespart) {
        String kategorie = budget.getKategorie();
        BigDecimal ist = ausgaben.getOrDefault(kategorie, BigDecimal.ZERO);
        BigDecimal spielraum = budget.getLimit().subtract(ist);
        if (spielraum.signum() < 0) {
            return;
        }
        String haltung = kategorieRepository.findById(kategorie)
                .map(KategorieEntity::asHaltung)
                .orElse(Haltung.FLEXIBEL);
        if (Haltung.BEWUSST_MEHR.equals(haltung)) {
            if (spielraum.signum() > 0) {
                empfehlungen.add(new CoachingEmpfehlung(kategorie, haltung, ist, budget.getLimit(),
                        spielraum, tipp(kategorie, spielraum)));
            }
        } else if (Haltung.SPAREN.equals(haltung)) {
            gutGespart.add(new CoachingErspart(kategorie, haltung, ist, budget.getLimit(), spielraum));
        } else {
            budgetsUebrig.put(kategorie, spielraum);
        }
    }

    private String tipp(String kategorie, BigDecimal spielraum) {
        return kategorie + " verträgt " + String.format(Locale.GERMANY, "%.2f", spielraum)
                + " € mehr — bewusst genießen";
    }
}
