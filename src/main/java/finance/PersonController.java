package finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/personen")
public class PersonController {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final PersonRepository repository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUser authenticatedUser;

    public PersonController(PersonRepository repository, AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder, AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<Person> create(@RequestBody PersonInput input) {
        if (appUserRepository.count() > 0 && !authenticatedUser.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (input.name() == null || input.name().isBlank()
                || input.passwort() == null || input.passwort().isBlank()
                || input.passwort().length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().build();
        }
        AppUserEntity user = appUserRepository.findByUsername(input.name())
                .orElseGet(() -> new AppUserEntity(input.name(), null));
        user.setPasswordHash(passwordEncoder.encode(input.passwort()));
        appUserRepository.save(user);
        return repository.findByName(input.name())
                .<ResponseEntity<Person>>map(existing -> ResponseEntity.ok(existing.asPerson()))
                .orElseGet(() -> {
                    PersonEntity saved = repository.save(new PersonEntity(input.name()));
                    return ResponseEntity.status(HttpStatus.CREATED).body(saved.asPerson());
                });
    }

    @GetMapping
    public List<Person> list() {
        if (appUserRepository.count() > 0 && !authenticatedUser.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return repository.findAll().stream().map(PersonEntity::asPerson).toList();
    }
}
