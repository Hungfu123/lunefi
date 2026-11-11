package finance;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "lunefi.demo-seed", havingValue = "true")
public class DemoSeed implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder encoder;

    public DemoSeed(PersonRepository personRepository, AppUserRepository appUserRepository, PasswordEncoder encoder) {
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        List<PersonEntity> allPersons = personRepository.findAll();
        if (allPersons.isEmpty()) {
            return;
        }
        for (PersonEntity person : allPersons) {
            String password = System.getenv().getOrDefault(
                    "LUNEFI_DEMO_PASSWORT_" + person.getName().toUpperCase(),
                    "lunefi-dev");
            appUserRepository.save(new AppUserEntity(person.getName(), encoder.encode(password)));
        }
    }
}
