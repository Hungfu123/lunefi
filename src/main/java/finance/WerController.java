package finance;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WerController {

    private final AngemeldetePerson angemeldetePerson;

    public WerController(AngemeldetePerson angemeldetePerson) {
        this.angemeldetePerson = angemeldetePerson;
    }

    @GetMapping("/api/wer")
    public Map<String, String> wer() {
        return Map.of("name", angemeldetePerson.name());
    }
}
