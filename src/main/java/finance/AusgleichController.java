package finance;

import java.time.YearMonth;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ausgleich")
public class AusgleichController {

    private final AusgleichService service;

    public AusgleichController(AusgleichService service) {
        this.service = service;
    }

    @GetMapping("/{jahr}-{monat}")
    public AusgleichMitPositionen settlement(@PathVariable int jahr, @PathVariable int monat) {
        try {
            return service.settlement(YearMonth.of(jahr, monat));
        } catch (java.time.DateTimeException ausnahme) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verlauf")
    public List<AusgleichMonat> history(@RequestParam(defaultValue = "6") int monate) {
        if (monate < 1) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        return service.history(monate);
    }
}
