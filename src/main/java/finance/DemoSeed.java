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

    private final PersonRepository personen;
    private final AppUserRepository nutzer;
    private final PasswordEncoder encoder;

    public DemoSeed(PersonRepository personen, AppUserRepository nutzer, PasswordEncoder encoder) {
        this.personen = personen;
        this.nutzer = nutzer;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (nutzer.count() > 0) {
            return;
        }
        List<PersonEntity> alle = personen.findAll();
        if (alle.isEmpty()) {
            return;
        }
        for (PersonEntity person : alle) {
            String passwort = System.getenv().getOrDefault(
                    "LUNEFI_DEMO_PASSWORT_" + person.getName().toUpperCase(),
                    "lunefi-dev");
            nutzer.save(new AppUserEntity(person.getName(), encoder.encode(passwort)));
        }
    }
}
