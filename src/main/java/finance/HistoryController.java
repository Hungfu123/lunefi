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
public class HistoryController {

    private static final int DEFAULT_MONTHS = 3;
    private static final int MAX_MONTHS = 24;

    private final EntryRepository entryRepository;
    private final EntryParts entryParts;
    private final Clock clock;
    private final AuthenticatedUser authenticatedUser;

    public HistoryController(EntryRepository entryRepository, EntryParts entryParts, Clock clock,
            AuthenticatedUser authenticatedUser) {
        this.entryRepository = entryRepository;
        this.entryParts = entryParts;
        this.clock = clock;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/api/verlauf")
    public List<HistoryMonth> history(@RequestParam(defaultValue = "" + DEFAULT_MONTHS) int monate,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (monate < 1) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }
        int capped = Math.min(monate, MAX_MONTHS);

        List<LineItem> filtered = entryParts.lineItems(entryRepository.findAll()).stream()
                .filter(item -> person == null || person.equals(item.person()))
                .filter(item -> !Boolean.TRUE.equals(gemeinsam) || item.shared())
                .sorted(Comparator.comparing(LineItem::date))
                .toList();

        YearMonth currentMonth = YearMonth.now(clock);
        List<HistoryMonth> history = new ArrayList<>();
        for (int back = capped - 1; back >= 0; back--) {
            YearMonth month = currentMonth.minusMonths(back);
            BigDecimal income = BigDecimal.ZERO.setScale(2);
            BigDecimal expenses = BigDecimal.ZERO.setScale(2);
            for (LineItem item : filtered) {
                if (!YearMonth.from(item.date()).equals(month)) {
                    continue;
                }
                if (item.amount().signum() < 0) {
                    expenses = expenses.add(item.amount().abs());
                } else {
                    income = income.add(item.amount().abs());
                }
            }
            history.add(new HistoryMonth(month.toString(), income, expenses,
                    income.subtract(expenses)));
        }
        return history;
    }
}
