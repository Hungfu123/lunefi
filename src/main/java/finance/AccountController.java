package finance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/konten")
public class AccountController {

    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;
    private final PersonRepository personRepository;
    private final AuthenticatedUser authenticatedUser;

    public AccountController(AccountRepository accountRepository, EntryRepository entryRepository,
            PersonRepository personRepository, AuthenticatedUser authenticatedUser) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.personRepository = personRepository;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody Account account) {
        if (account.person() != null && !account.person().isBlank()) {
            authenticatedUser.requireOwnPerson(account.person());
        }
        if (account.name() == null || account.name().isBlank()
                || account.person() == null || account.person().isBlank()
                || account.startSaldo() == null
                || !personRepository.existsByName(account.person())
                || accountRepository.existsByName(account.name())) {
            return ResponseEntity.badRequest().build();
        }
        AccountEntity saved = accountRepository
                .save(new AccountEntity(account.name(), account.person(), account.startSaldo()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Account(saved.getId(), saved.getName(), saved.getPerson(),
                        saved.getStartSaldo(), saved.getStartSaldo()));
    }

    @GetMapping
    public List<Account> list(@RequestParam(required = false) String person) {
        if (person != null) {
            authenticatedUser.requireOwnPerson(person);
        }
        List<Account> result = new ArrayList<>();
        for (AccountEntity account : accountRepository.findAll()) {
            if (person != null && !person.equals(account.getPerson())) {
                continue;
            }
            result.add(new Account(account.getId(), account.getName(), account.getPerson(),
                    account.getStartSaldo(), currentBalance(account)));
        }
        return result;
    }

    private BigDecimal currentBalance(AccountEntity account) {
        BigDecimal total = account.getStartSaldo();
        for (EntryEntity entry : entryRepository.findAll()) {
            if (account.getName().equals(entry.getKonto())) {
                total = total.add(entry.getBetrag());
            }
        }
        return total;
    }
}
