package finance;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InitialSetupController {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialSetupController(PersonRepository personRepository, AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/ersteinrichtung")
    public ResponseEntity<Void> initialSetup(@RequestBody InitialSetupInput input) {
        if (appUserRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<String, String> passwords = input.passwoerter();
        if (passwords == null || passwords.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        for (Map.Entry<String, String> entry : passwords.entrySet()) {
            if (personRepository.findByName(entry.getKey()).isEmpty()
                    || entry.getValue() == null || entry.getValue().length() < MIN_PASSWORD_LENGTH) {
                return ResponseEntity.badRequest().build();
            }
        }
        for (Map.Entry<String, String> entry : passwords.entrySet()) {
            appUserRepository.save(new AppUserEntity(entry.getKey(), passwordEncoder.encode(entry.getValue())));
        }
        return ResponseEntity.ok().build();
    }
}
