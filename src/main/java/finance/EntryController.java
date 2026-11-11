package finance;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eintraege")
public class EntryController {

    private final EntryRepository repository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final AuthenticatedUser authenticatedUser;

    public EntryController(EntryRepository repository, PersonRepository personRepository,
            AccountRepository accountRepository, AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<Entry> capture(@RequestBody Entry entry) {
        if (!isOwnOrShared(entry)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isValid(entry)) {
            return ResponseEntity.badRequest().build();
        }
        EntryEntity saved = repository.save(new EntryEntity(
                entry.datum(), entry.betrag(), entry.kategorie(),
                entry.person(), Boolean.TRUE.equals(entry.gemeinsam()), entry.zahler(),
                entry.konto()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.asEntry());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Entry> update(@PathVariable long id, @RequestBody Entry entry) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwnOrShared(entry)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        EntryEntity existing = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(existing.getGemeinsam())) {
            authenticatedUser.requireOwnPerson(existing.getPerson());
        }
        if (!isValid(entry)) {
            return ResponseEntity.badRequest().build();
        }
        existing.update(entry.datum(), entry.betrag(), entry.kategorie(),
                entry.person(), Boolean.TRUE.equals(entry.gemeinsam()), entry.zahler(),
                entry.konto());
        return ResponseEntity.ok(repository.save(existing).asEntry());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        EntryEntity existing = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(existing.getGemeinsam())) {
            authenticatedUser.requireOwnPerson(existing.getPerson());
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Entry> list(@RequestParam(required = false) String monat,
            @RequestParam(required = false) String person,
            @RequestParam(required = false) Boolean gemeinsam) {
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }
        YearMonth period = monat == null ? null : monthAsYearMonth(monat);
        return repository.findAll().stream()
                .map(EntryEntity::asEntry)
                .filter(entry -> period == null || YearMonth.from(entry.datum()).equals(period))
                .filter(entry -> person == null || person.equals(entry.person()))
                .filter(entry -> gemeinsam == null
                        || gemeinsam.equals(Boolean.TRUE.equals(entry.gemeinsam())))
                .toList();
    }

    private boolean isOwnOrShared(Entry entry) {
        boolean personal = entry.person() != null && !entry.person().isBlank();
        return !personal || Objects.equals(authenticatedUser.name(), entry.person());
    }

    private boolean isValid(Entry entry) {
        return entry.kategorie() != null && !entry.kategorie().isBlank()
                && entry.datum() != null
                && entry.betrag() != null
                && exactlyOneAssignment(entry)
                && payerIfShared(entry)
                && (entry.konto() == null || accountRepository.existsByName(entry.konto()));
    }

    private boolean exactlyOneAssignment(Entry entry) {
        boolean personSet = entry.person() != null && !entry.person().isBlank();
        return personSet ^ Boolean.TRUE.equals(entry.gemeinsam());
    }

    private boolean payerIfShared(Entry entry) {
        if (!Boolean.TRUE.equals(entry.gemeinsam())) {
            return true;
        }
        return entry.zahler() != null && !entry.zahler().isBlank()
                && personRepository.existsByName(entry.zahler());
    }

    private YearMonth monthAsYearMonth(String monat) {
        try {
            return YearMonth.parse(monat);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
