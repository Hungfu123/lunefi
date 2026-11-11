package finance;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErsteinrichtungController {

    private static final int MIN_PASSWORT_LAENGE = 6;

    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public ErsteinrichtungController(PersonRepository personRepository, AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/ersteinrichtung")
    public ResponseEntity<Void> ersteinrichtung(@RequestBody ErsteinrichtungEingabe eingabe) {
        if (appUserRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<String, String> passwoerter = eingabe.passwoerter();
        if (passwoerter == null || passwoerter.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        for (Map.Entry<String, String> eintrag : passwoerter.entrySet()) {
            if (personRepository.findByName(eintrag.getKey()).isEmpty()
                    || eintrag.getValue() == null || eintrag.getValue().length() < MIN_PASSWORT_LAENGE) {
                return ResponseEntity.badRequest().build();
            }
        }
        for (Map.Entry<String, String> eintrag : passwoerter.entrySet()) {
            appUserRepository.save(new AppUserEntity(eintrag.getKey(), passwordEncoder.encode(eintrag.getValue())));
        }
        return ResponseEntity.ok().build();
    }
}
