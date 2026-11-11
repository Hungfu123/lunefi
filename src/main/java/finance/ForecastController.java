package finance;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ForecastController {

    private static final int DEFAULT_MONTHS = 3;
    private static final int MIN_MONTHS = 2;
    private static final int MAX_MONTHS = 24;

    private final EntryRepository entryRepository;
    private final ForecastService forecastService;
    private final AuthenticatedUser authenticatedUser;

    public ForecastController(EntryRepository entryRepository, ForecastService forecastService,
            AuthenticatedUser authenticatedUser) {
        this.entryRepository = entryRepository;
        this.forecastService = forecastService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/api/prognose")
    public Forecast forecast(@RequestParam(defaultValue = "" + DEFAULT_MONTHS) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (person != null && Boolean.TRUE.equals(gemeinsam)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }
        if (monate < MIN_MONTHS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        int capped = Math.min(monate, MAX_MONTHS);

        List<Entry> filtered = entryRepository.findAll().stream()
                .map(EntryEntity::asEntry)
                .filter(entry -> person == null || person.equals(entry.person()))
                .filter(entry -> !Boolean.TRUE.equals(gemeinsam) || Boolean.TRUE.equals(entry.gemeinsam()))
                .sorted(Comparator.comparing(Entry::datum))
                .toList();

        return forecastService.forecast(filtered, capped);
    }
}
