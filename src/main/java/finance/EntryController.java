package finance;

import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eintraege")
public class EntryController {

    private final EntryRepository repository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final EntryParts entryParts;
    private final AuthenticatedUser authenticatedUser;

    public EntryController(EntryRepository repository, PersonRepository personRepository,
            AccountRepository accountRepository, EntryParts entryParts,
            AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.entryParts = entryParts;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Entry> capture(@RequestBody Entry entry) {
        if (hasParts(entry)) {
            return captureWithParts(entry);
        }
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
        return ResponseEntity.status(HttpStatus.CREATED).body(entryParts.toApi(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Entry> update(@PathVariable long id, @RequestBody Entry entry) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        EntryEntity existing = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(existing.getGemeinsam())) {
            authenticatedUser.requireOwnPerson(existing.getPerson());
        }
        if (hasParts(entry)) {
            return updateWithParts(existing, entry);
        }
        if (!isOwnOrShared(entry)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isValid(entry)) {
            return ResponseEntity.badRequest().build();
        }
        existing.update(entry.datum(), entry.betrag(), entry.kategorie(),
                entry.person(), Boolean.TRUE.equals(entry.gemeinsam()), entry.zahler(),
                entry.konto());
        entryParts.clearParts(existing.getId());
        return ResponseEntity.ok(entryParts.toApi(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        EntryEntity existing = repository.findById(id).orElseThrow();
        if (!Boolean.TRUE.equals(existing.getGemeinsam())) {
            authenticatedUser.requireOwnPerson(existing.getPerson());
        }
        entryParts.clearParts(id);
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
                .filter(entry -> period == null || YearMonth.from(entry.getDatum()).equals(period))
                .filter(entry -> person == null || person.equals(entry.getPerson()))
                .filter(entry -> gemeinsam == null
                        || gemeinsam.equals(Boolean.TRUE.equals(entry.getGemeinsam())))
                .map(entryParts::toApi)
                .toList();
    }

    private boolean hasParts(Entry entry) {
        return entry.teile() != null && !entry.teile().isEmpty();
    }

    private ResponseEntity<Entry> captureWithParts(Entry entry) {
        if (!partsAreValid(entry)) {
            return ResponseEntity.badRequest().build();
        }
        if (!privatePartsAreOwn(entry)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        EntryEntity saved = repository.save(entityFromParts(entry, entry.betrag()));
        entryParts.replaceParts(saved.getId(), entry.teile());
        return ResponseEntity.status(HttpStatus.CREATED).body(entryParts.toApi(saved));
    }

    private ResponseEntity<Entry> updateWithParts(EntryEntity existing, Entry entry) {
        if (!partsAreValid(entry)) {
            return ResponseEntity.badRequest().build();
        }
        if (!privatePartsAreOwn(entry) || !keepsForeignPrivateParts(existing, entry)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        EntryEntity updated = entityFromParts(entry, entry.betrag());
        existing.update(updated.getDatum(), updated.getBetrag(), updated.getKategorie(),
                updated.getPerson(), updated.getGemeinsam(), updated.getZahler(),
                updated.getKonto());
        EntryEntity saved = repository.save(existing);
        entryParts.replaceParts(saved.getId(), entry.teile());
        return ResponseEntity.ok(entryParts.toApi(saved));
    }

    private boolean keepsForeignPrivateParts(EntryEntity existing, Entry entry) {
        for (EntryPart previous : entryParts.partsFor(existing.getId())) {
            if (Boolean.TRUE.equals(previous.gemeinsam())
                    || Objects.equals(previous.person(), authenticatedUser.name())) {
                continue;
            }
            boolean kept = entry.teile().stream().anyMatch(part ->
                    Boolean.TRUE.equals(part.gemeinsam()) ? false
                            : Objects.equals(part.person(), previous.person())
                                    && part.kategorie() != null && part.kategorie().equals(previous.kategorie())
                                    && part.betrag() != null && part.betrag().compareTo(previous.betrag()) == 0);
            if (!kept) {
                return false;
            }
        }
        return true;
    }

    private EntryEntity entityFromParts(Entry entry, BigDecimal amount) {
        EntryPart primary = entryParts.primary(entry.teile());
        return new EntryEntity(entry.datum(), amount, primary.kategorie(), primary.person(),
                Boolean.TRUE.equals(primary.gemeinsam()), entry.zahler(), entry.konto());
    }

    private boolean partsAreValid(Entry entry) {
        if (!entryParts.isValid(entry.teile(), entry.betrag())) {
            return false;
        }
        if (entry.datum() == null) {
            return false;
        }
        if (entry.konto() != null && !accountRepository.existsByName(entry.konto())) {
            return false;
        }
        if (entryParts.anyShared(entry.teile())) {
            return entry.zahler() != null && !entry.zahler().isBlank()
                    && personRepository.existsByName(entry.zahler());
        }
        return true;
    }

    private boolean privatePartsAreOwn(Entry entry) {
        for (EntryPart part : entry.teile()) {
            if (!Boolean.TRUE.equals(part.gemeinsam())
                    && !Objects.equals(authenticatedUser.name(), part.person())) {
                return false;
            }
        }
        return true;
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

    private YearMonth monthAsYearMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
