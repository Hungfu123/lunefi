package finance;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoAmIController {

    private final AuthenticatedUser authenticatedUser;

    public WhoAmIController(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/api/wer")
    public Map<String, String> whoAmI() {
        return Map.of("name", authenticatedUser.name());
    }
}
