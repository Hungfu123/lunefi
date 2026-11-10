package finance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monatsuebersicht")
public class MonatsuebersichtController {

    private final EintragSpeicher speicher;

    public MonatsuebersichtController(EintragSpeicher speicher) {
        this.speicher = speicher;
    }

    @GetMapping("/{jahr}-{monat}")
    public Monatsuebersicht fuerMonat(@PathVariable int jahr, @PathVariable int monat) {
        YearMonth zeitraum = YearMonth.of(jahr, monat);
        List<Eintrag> imMonat = speicher.alle().stream()
                .filter(eintrag -> YearMonth.from(eintrag.datum()).equals(zeitraum))
                .sorted(Comparator.comparing(Eintrag::datum))
                .toList();

        Map<String, BigDecimal> ausgaben = new HashMap<>();
        Map<String, BigDecimal> einnahmen = new HashMap<>();
        for (Eintrag eintrag : imMonat) {
            Map<String, BigDecimal> ziel = eintrag.betrag().signum() < 0 ? ausgaben : einnahmen;
            ziel.merge(eintrag.kategorie(), eintrag.betrag().abs(), BigDecimal::add);
        }
        return new Monatsuebersicht(ausgaben, einnahmen, imMonat);
    }
}
