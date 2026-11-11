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

public class ErsteinrichtungSteps {

    @Autowired
    private finance.PersonRepository personRepository;

    @Autowired
    private finance.AppUserRepository appUserRepository;

    @Autowired
    private Sitzungen sitzungen;

    @Autowired
    @Qualifier("unangemeldetRest")
    private TestRestTemplate unangemeldet;

    private HttpStatusCode einrichtungStatus;

    @Angenommen("die Person {string} ohne Passwort existiert")
    public void personWithoutAppUser(String name) {
        appUserRepository.deleteAll();
        personRepository.findByName(name)
                .orElseGet(() -> personRepository.save(new finance.PersonEntity(name)));
    }

    @Angenommen("die Personen {string} und {string} ohne Passwort existieren")
    public void peopleWithoutAppUser(String erste, String zweite) {
        personWithoutAppUser(erste);
        personWithoutAppUser(zweite);
    }

    @Wenn("jemand ohne Anmeldung die Passwörter vergibt")
    public void anonymousAssignsPasswords(DataTable tabelle) {
        unangemeldet.getForEntity("/api/personen", String.class);
        Map<String, String> passwoerter = new LinkedHashMap<>();
        for (Map<String, String> zeile : tabelle.asMaps()) {
            passwoerter.put(zeile.get("Person"), zeile.get("Passwort"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> antwort = unangemeldet.postForEntity("/api/ersteinrichtung",
                new HttpEntity<>(Map.of("passwoerter", passwoerter), headers), String.class);
        einrichtungStatus = antwort.getStatusCode();
    }

    @Dann("ist die nachträgliche Einrichtung erfolgreich")
    public void setupSuccessful() {
        assertThat(einrichtungStatus).isEqualTo(HttpStatus.OK);
    }

    @Dann("kann sich {string} danach mit dem Passwort {string} anmelden")
    public void loginAfterSetup(String name, String passwort) {
        TestRestTemplate vorlage = sitzungen.vorlage();
        vorlage.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", passwort);
        ResponseEntity<String> antwort = vorlage.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird die Nachrüstung wegen bestehender Einrichtung abgewiesen")
    public void setupRejectedAlreadyConfigured() {
        assertThat(einrichtungStatus).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Dann("wird die Nachrüstung wegen ungültiger Angaben abgewiesen")
    public void setupRejectedInvalid() {
        assertThat(einrichtungStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
