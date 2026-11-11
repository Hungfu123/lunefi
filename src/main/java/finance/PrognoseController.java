package finance;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PrognoseController {

    private static final int STANDARD_MONATE = 3;
    private static final int MIN_MONATE = 2;
    private static final int MAX_MONATE = 24;

    private final EintragRepository eintragRepository;
    private final PrognoseService prognoseService;

    public PrognoseController(EintragRepository eintragRepository, PrognoseService prognoseService) {
        this.eintragRepository = eintragRepository;
        this.prognoseService = prognoseService;
    }

    @GetMapping("/api/prognose")
    public Prognose prognose(@RequestParam(defaultValue = "" + STANDARD_MONATE) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (person != null && Boolean.TRUE.equals(gemeinsam)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (monate < MIN_MONATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        int begrenzt = Math.min(monate, MAX_MONATE);

        List<Eintrag> gefiltert = eintragRepository.findAll().stream()
                .map(EintragEntity::asEntry)
                .filter(eintrag -> person == null || person.equals(eintrag.person()))
                .filter(eintrag -> !Boolean.TRUE.equals(gemeinsam) || Boolean.TRUE.equals(eintrag.gemeinsam()))
                .sorted(Comparator.comparing(Eintrag::datum))
                .toList();

        return prognoseService.erstelle(gefiltert, begrenzt);
    }
}
