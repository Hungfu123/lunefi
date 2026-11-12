package finance.bdd;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

public class GoogleLoginSteps {

    private static final String TEST_PASSWORD = "geheim123";

    @Autowired
    private Sessions sessions;

    @Autowired
    private finance.AppUserRepository appUserRepository;

    @Value("${lunefi.google.zuordnung:}")
    private String aktiveZuordnung;

    private TestRestTemplate template;
    private ResponseEntity<String> loginResponse;

    @Angenommen("die App mit den Nutzern Alex und Kim eingerichtet ist")
    public void appWithUsersSetUp() {
        appUserRepository.deleteAll();
        template = sessions.template();
        template.getForEntity("/api/personen", String.class);
        createPerson("Alex", TEST_PASSWORD);
        loginWithPassword("Alex", TEST_PASSWORD);
        createPerson("Kim", TEST_PASSWORD);
    }

    @Angenommen("die Google-Zuordnung {string} hinterlegt ist")
    public void googleMappingSetUp(String zuordnung) {
        for (String pair : zuordnung.split(",")) {
            String[] parts = pair.split("=", 2);
            assertThat(parts).hasSize(2);
            assertThat(aktiveZuordnung).contains(pair);
        }
    }

    @Wenn("sich jemand mit einem gültigen Google-Token für {string} anmeldet")
    public void googleLogin(String email) {
        googleLoginWithCredential("google:" + email);
    }

    @Wenn("sich jemand mit einem Google-Token für die nicht verifizierte Adresse {string} anmeldet")
    public void googleLoginUnverified(String email) {
        googleLoginWithCredential("google-offen:" + email);
    }

    @Wenn("sich jemand mit dem ungültigen Google-Token {string} anmeldet")
    public void googleLoginInvalid(String credential) {
        googleLoginWithCredential(credential);
    }

    @Dann("ist die Anmeldung erfolgreich und der gemeldete Name lautet {string}")
    public void googleLoginSuccessful(String name) {
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).contains("\"name\":\"" + name + "\"");
        ResponseEntity<String> who = template.getForEntity("/api/wer", String.class);
        assertThat(who.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(who.getBody()).contains("\"name\":\"" + name + "\"");
    }

    @Dann("wird die Anmeldung abgewiesen und es entsteht keine Sitzung")
    public void googleLoginRejected() {
        assertThat(loginResponse.getStatusCode().is4xxClientError()).isTrue();
        ResponseEntity<String> who = template.getForEntity("/api/wer", String.class);
        assertThat(who.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void googleLoginWithCredential(String credential) {
        template = sessions.template();
        template.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        loginResponse = template.postForEntity("/api/auth/google",
                new HttpEntity<>(Map.of("credential", credential), headers), String.class);
    }

    private void createPerson(String name, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        template.getForEntity("/api/personen", String.class);
        ResponseEntity<String> response = template.postForEntity("/api/personen",
                new HttpEntity<>(Map.of("name", name, "passwort", password), headers), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Status: %s Body: %s", response.getStatusCode(), response.getBody())
                .isTrue();
    }

    private void loginWithPassword(String name, String password) {
        template.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "username=" + name + "&password=" + password;
        ResponseEntity<String> response = template.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
