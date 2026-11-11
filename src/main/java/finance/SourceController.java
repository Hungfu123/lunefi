package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quellen")
public class SourceController {

    private final SourceRepository sourceRepository;
    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;
    private final LearningRuleRepository learningRuleRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY);

    public SourceController(SourceRepository sourceRepository, AccountRepository accountRepository,
            EntryRepository entryRepository, LearningRuleRepository learningRuleRepository) {
        this.sourceRepository = sourceRepository;
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.learningRuleRepository = learningRuleRepository;
    }

    @PostMapping
    public ResponseEntity<Source> create(@RequestBody SourceInput input) {
        if (input.name() == null || input.name().isBlank()
                || input.art() == null || input.art().isBlank()
                || input.konto() == null || input.konto().isBlank()
                || !accountRepository.existsByName(input.konto())
                || sourceRepository.existsByName(input.name())) {
            return ResponseEntity.badRequest().build();
        }
        SourceEntity saved = sourceRepository.save(new SourceEntity(input.name(), input.art(), input.konto()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Source(saved.getId(), saved.getName(), saved.getArt(),
                        saved.getKonto(), saved.getZustand()));
    }

    @GetMapping
    public List<Source> list() {
        List<Source> result = new ArrayList<>();
        for (SourceEntity source : sourceRepository.findAll()) {
            result.add(new Source(source.getId(), source.getName(), source.getArt(),
                    source.getKonto(), source.getZustand()));
        }
        return result;
    }

    @PostMapping("/{id}/import")
    public ResponseEntity<ImportResult> importEntries(@PathVariable Long id, @RequestBody ImportRequest request) {
        SourceEntity source = sourceRepository.findById(id).orElse(null);
        if (source == null) {
            return ResponseEntity.notFound().build();
        }
        AccountEntity account = accountRepository.findByName(source.getKonto()).orElse(null);
        if (account == null) {
            return ResponseEntity.badRequest().build();
        }

        int created = 0;
        int skipped = 0;
        for (TransactionDto transaction : request.umsaetze()) {
            if (entryRepository.existsByQuelleIdAndExterneKennung(id, transaction.externeKennung())) {
                skipped++;
                continue;
            }
            EntryEntity entry = new EntryEntity(
                    LocalDate.parse(transaction.datum(), DATE_FORMAT),
                    amountFrom(transaction.betrag()),
                    categoryFor(transaction.verwendungszweck()),
                    account.getPerson(),
                    false,
                    null,
                    account.getName());
            entry.setQuelleId(id);
            entry.setExterneKennung(transaction.externeKennung());
            entry.setVerwendungszweck(transaction.verwendungszweck());
            entryRepository.save(entry);
            created++;
        }
        return ResponseEntity.ok(new ImportResult(created, skipped));
    }

    private String categoryFor(String verwendungszweck) {
        if (verwendungszweck != null) {
            String upper = verwendungszweck.toUpperCase(Locale.GERMANY);
            for (LearningRuleEntity rule : learningRuleRepository.findAll()) {
                if (upper.contains(rule.getKennung().toUpperCase(Locale.GERMANY))) {
                    return rule.getKategorie();
                }
            }
        }
        return "Import";
    }

    private BigDecimal amountFrom(String text) {
        String normalized = text.replace(".", "").replace(',', '.');
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return new BigDecimal(normalized);
    }

    public record SourceInput(String name, String art, String konto) {
    }

    public record TransactionDto(String externeKennung, String datum, String betrag, String verwendungszweck) {
    }

    public record ImportRequest(List<TransactionDto> umsaetze) {
    }

    public record ImportResult(int neu, int uebersprungen) {
    }
}
