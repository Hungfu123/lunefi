package finance;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StatistikController {

    private static final int STANDARD_MONATE = 6;
    private static final int MAX_MONATE = 24;

    private final EintragRepository eintragRepository;
    private final StatistikService statistikService;
    private final AngemeldetePerson angemeldetePerson;

    public StatistikController(EintragRepository eintragRepository, StatistikService statistikService,
            AngemeldetePerson angemeldetePerson) {
        this.eintragRepository = eintragRepository;
        this.statistikService = statistikService;
        this.angemeldetePerson = angemeldetePerson;
    }

    @GetMapping("/api/statistik/kategorie/{kategorie}")
    public StatistikVerlauf kategorie(@PathVariable String kategorie,
            @RequestParam(defaultValue = "" + STANDARD_MONATE) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        pruefeSicht(person, gemeinsam);
        if (monate < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        int begrenzt = Math.min(monate, MAX_MONATE);
        List<Eintrag> gefiltert = gefilterteEintraege(person, gemeinsam);
        return statistikService.verlaufJeKategorie(gefiltert, kategorie, begrenzt);
    }

    @GetMapping("/api/statistik/sparpotenzial")
    public Sparpotenzial sparpotenzial(@RequestParam String monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        YearMonth zeitraum;
        try {
            zeitraum = YearMonth.parse(monat);
        } catch (DateTimeParseException ausnahme) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        pruefeSicht(person, gemeinsam);
        List<Eintrag> gefiltert = gefilterteEintraege(person, gemeinsam);
        return statistikService.sparpotenzial(gefiltert, zeitraum);
    }

    private void pruefeSicht(String person, Boolean gemeinsam) {
        if (person != null && Boolean.TRUE.equals(gemeinsam)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            angemeldetePerson.pruefeEigenePerson(person);
        }
    }

    private List<Eintrag> gefilterteEintraege(String person, Boolean gemeinsam) {
        return eintragRepository.findAll().stream()
                .map(EintragEntity::asEntry)
                .filter(eintrag -> person == null || person.equals(eintrag.person()))
                .filter(eintrag -> !Boolean.TRUE.equals(gemeinsam) || Boolean.TRUE.equals(eintrag.gemeinsam()))
                .sorted(Comparator.comparing(Eintrag::datum))
                .toList();
    }
}
