package finance.bdd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.ObjectMapper;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

public class WhoAmISteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private Sessions sessions;

    @Autowired
    @Qualifier("unauthenticatedRest")
    private TestRestTemplate unauthenticated;

    private HttpStatusCode queryStatus;
    private String reportedName;

    @Wenn("{string} abfragt, wer angemeldet ist")
    public void loggedInUserAsksIdentity(String person) {
        ResponseEntity<String> response = sessions.forPerson(person).getForEntity("/api/wer", String.class);
        queryStatus = response.getStatusCode();
        reportedName = mapper.readTree(response.getBody()).path("name").asText();
    }

    @Wenn("jemand ohne Anmeldung abfragt, wer angemeldet ist")
    public void anonymousUserAsksIdentity() {
        ResponseEntity<String> response = unauthenticated.getForEntity("/api/wer", String.class);
        queryStatus = response.getStatusCode();
    }

    @Dann("lautet der gemeldete Name {string}")
    public void reportedName(String name) {
        assertThat(queryStatus).isEqualTo(HttpStatus.OK);
        assertThat(reportedName).isEqualTo(name);
    }

    @Dann("wird die Abfrage wegen fehlender Anmeldung abgewiesen")
    public void identityRejectedUnauthenticated() {
        assertThat(queryStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
