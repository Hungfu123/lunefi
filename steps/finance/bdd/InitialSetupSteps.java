package finance.bdd;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

public class InitialSetupSteps {

    @Autowired
    private finance.PersonRepository personRepository;

    @Autowired
    private finance.AppUserRepository appUserRepository;

    @Autowired
    private Sessions sessions;

    @Autowired
    @Qualifier("unauthenticatedRest")
    private TestRestTemplate unauthenticated;

    private HttpStatusCode setupStatus;

    @Angenommen("die Person {string} ohne Passwort existiert")
    public void personWithoutAppUser(String name) {
        appUserRepository.deleteAll();
        personRepository.findByName(name)
                .orElseGet(() -> personRepository.save(new finance.PersonEntity(name)));
    }

    @Angenommen("die Personen {string} und {string} ohne Passwort existieren")
    public void peopleWithoutAppUser(String first, String second) {
        personWithoutAppUser(first);
        personWithoutAppUser(second);
    }

    @Wenn("jemand ohne Anmeldung die Passwörter vergibt")
    public void anonymousAssignsPasswords(DataTable table) {
        unauthenticated.getForEntity("/api/personen", String.class);
        Map<String, String> passwords = new LinkedHashMap<>();
        for (Map<String, String> row : table.asMaps()) {
            passwords.put(row.get("Person"), row.get("Passwort"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = unauthenticated.postForEntity("/api/ersteinrichtung",
                new HttpEntity<>(Map.of("passwoerter", passwords), headers), String.class);
        setupStatus = response.getStatusCode();
    }

    @Dann("ist die nachträgliche Einrichtung erfolgreich")
    public void setupSuccessful() {
        assertThat(setupStatus).isEqualTo(HttpStatus.OK);
    }

    @Dann("kann sich {string} danach mit dem Passwort {string} anmelden")
    public void loginAfterSetup(String name, String password) {
        TestRestTemplate template = sessions.template();
        template.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", password);
        ResponseEntity<String> response = template.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird die Nachrüstung wegen bestehender Einrichtung abgewiesen")
    public void setupRejectedAlreadyConfigured() {
        assertThat(setupStatus).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Dann("wird die Nachrüstung wegen ungültiger Angaben abgewiesen")
    public void setupRejectedInvalid() {
        assertThat(setupStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
