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
public class StatisticsController {

    private static final int DEFAULT_MONTHS = 6;
    private static final int MAX_MONTHS = 24;

    private final EntryRepository entryRepository;
    private final EntryParts entryParts;
    private final StatisticsService statisticsService;
    private final AuthenticatedUser authenticatedUser;

    public StatisticsController(EntryRepository entryRepository, EntryParts entryParts,
            StatisticsService statisticsService, AuthenticatedUser authenticatedUser) {
        this.entryRepository = entryRepository;
        this.entryParts = entryParts;
        this.statisticsService = statisticsService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/api/statistik/kategorie/{kategorie}")
    public CategoryStatistics category(@PathVariable String kategorie,
            @RequestParam(defaultValue = "" + DEFAULT_MONTHS) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        checkView(person, gemeinsam);
        if (monate < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        int capped = Math.min(monate, MAX_MONTHS);
        List<LineItem> filtered = filteredItems(person, gemeinsam);
        return statisticsService.categoryHistory(filtered, kategorie, capped);
    }

    @GetMapping("/api/statistik/sparpotenzial")
    public SavingsPotential savingsPotential(@RequestParam String monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        YearMonth period;
        try {
            period = YearMonth.parse(monat);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        checkView(person, gemeinsam);
        List<LineItem> filtered = filteredItems(person, gemeinsam);
        return statisticsService.savingsPotential(filtered, period);
    }

    private void checkView(String person, Boolean gemeinsam) {
        if (person != null && Boolean.TRUE.equals(gemeinsam)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }
    }

    private List<LineItem> filteredItems(String person, Boolean gemeinsam) {
        return entryParts.lineItems(entryRepository.findAll()).stream()
                .filter(item -> person == null || person.equals(item.person()))
                .filter(item -> !Boolean.TRUE.equals(gemeinsam) || item.shared())
                .sorted(Comparator.comparing(LineItem::date))
                .toList();
    }
}
