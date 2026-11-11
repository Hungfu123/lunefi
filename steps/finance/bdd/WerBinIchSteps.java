package finance.bdd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.ObjectMapper;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

public class WerBinIchSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private Sitzungen sitzungen;

    @Autowired
    @Qualifier("unangemeldetRest")
    private TestRestTemplate unangemeldet;

    private HttpStatusCode abfrageStatus;
    private String gemeldeterName;

    @Wenn("{string} abfragt, wer angemeldet ist")
    public void loggedInUserAsksIdentity(String person) {
        ResponseEntity<String> antwort = sitzungen.fuer(person).getForEntity("/api/wer", String.class);
        abfrageStatus = antwort.getStatusCode();
        gemeldeterName = mapper.readTree(antwort.getBody()).path("name").asText();
    }

    @Wenn("jemand ohne Anmeldung abfragt, wer angemeldet ist")
    public void anonymousUserAsksIdentity() {
        ResponseEntity<String> antwort = unangemeldet.getForEntity("/api/wer", String.class);
        abfrageStatus = antwort.getStatusCode();
    }

    @Dann("lautet der gemeldete Name {string}")
    public void reportedName(String name) {
        assertThat(abfrageStatus).isEqualTo(HttpStatus.OK);
        assertThat(gemeldeterName).isEqualTo(name);
    }

    @Dann("wird die Abfrage wegen fehlender Anmeldung abgewiesen")
    public void identityRejectedUnauthenticated() {
        assertThat(abfrageStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
