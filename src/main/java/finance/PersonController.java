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

    private static final int MIN_PASSWORT_LAENGE = 6;

    private final PersonRepository repository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AngemeldetePerson angemeldetePerson;

    public PersonController(PersonRepository repository, AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder, AngemeldetePerson angemeldetePerson) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.angemeldetePerson = angemeldetePerson;
    }

    @PostMapping
    public ResponseEntity<Person> create(@RequestBody PersonEingabe eingabe) {
        if (appUserRepository.count() > 0 && !angemeldetePerson.istAngemeldet()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (eingabe.name() == null || eingabe.name().isBlank()
                || eingabe.passwort() == null || eingabe.passwort().isBlank()
                || eingabe.passwort().length() < MIN_PASSWORT_LAENGE) {
            return ResponseEntity.badRequest().build();
        }
        AppUserEntity nutzer = appUserRepository.findByUsername(eingabe.name())
                .orElseGet(() -> new AppUserEntity(eingabe.name(), null));
        nutzer.setPasswordHash(passwordEncoder.encode(eingabe.passwort()));
        appUserRepository.save(nutzer);
        return repository.findByName(eingabe.name())
                .<ResponseEntity<Person>>map(besteht -> ResponseEntity.ok(besteht.asPerson()))
                .orElseGet(() -> {
                    PersonEntity gespeichert = repository.save(new PersonEntity(eingabe.name()));
                    return ResponseEntity.status(HttpStatus.CREATED).body(gespeichert.asPerson());
                });
    }

    @GetMapping
    public List<Person> list() {
        return repository.findAll().stream().map(PersonEntity::asPerson).toList();
    }
}
