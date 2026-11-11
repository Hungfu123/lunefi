package finance;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class VerlaufController {

    private static final int STANDARD_MONATE = 3;
    private static final int MAX_MONATE = 24;

    private final EintragRepository eintragRepository;
    private final Clock uhr;
    private final AngemeldetePerson angemeldetePerson;

    public VerlaufController(EintragRepository eintragRepository, Clock uhr, AngemeldetePerson angemeldetePerson) {
        this.eintragRepository = eintragRepository;
        this.uhr = uhr;
        this.angemeldetePerson = angemeldetePerson;
    }

    @GetMapping("/api/verlauf")
    public List<VerlaufMonat> verlauf(@RequestParam(defaultValue = "" + STANDARD_MONATE) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (monate < 1) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            angemeldetePerson.pruefeEigenePerson(person);
        }
        int begrenzt = Math.min(monate, MAX_MONATE);

        List<Eintrag> gefiltert = eintragRepository.findAll().stream()
                .map(EintragEntity::asEntry)
                .filter(eintrag -> person == null || person.equals(eintrag.person()))
                .filter(eintrag -> !Boolean.TRUE.equals(gemeinsam) || Boolean.TRUE.equals(eintrag.gemeinsam()))
                .sorted(Comparator.comparing(Eintrag::datum))
                .toList();

        YearMonth aktuellerMonat = YearMonth.now(uhr);
        List<VerlaufMonat> verlauf = new ArrayList<>();
        for (int zurueck = begrenzt - 1; zurueck >= 0; zurueck--) {
            YearMonth monat = aktuellerMonat.minusMonths(zurueck);
            BigDecimal einnahmen = BigDecimal.ZERO.setScale(2);
            BigDecimal ausgaben = BigDecimal.ZERO.setScale(2);
            for (Eintrag eintrag : gefiltert) {
                if (!YearMonth.from(eintrag.datum()).equals(monat)) {
                    continue;
                }
                if (eintrag.betrag().signum() < 0) {
                    ausgaben = ausgaben.add(eintrag.betrag().abs());
                } else {
                    einnahmen = einnahmen.add(eintrag.betrag().abs());
                }
            }
            verlauf.add(new VerlaufMonat(monat.toString(), einnahmen, ausgaben,
                    einnahmen.subtract(ausgaben)));
        }
        return verlauf;
    }
}
