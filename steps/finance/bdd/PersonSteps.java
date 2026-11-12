package finance.bdd;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    private org.springframework.http.HttpStatusCode anonStatus;
    private String anonBody;

    @Wenn("jemand ohne Anmeldung die Personenliste abruft")
    public void anonymousPersonList() {
        TestRestTemplate anon = sessions.template();
        ResponseEntity<String> response = anon.getForEntity("/api/personen", String.class);
        anonStatus = response.getStatusCode();
        anonBody = response.getBody();
    }

    @Dann("zeigt die Personenliste die Person {string}")
    public void anonymousListShows(String name) {
        assertThat(anonStatus).isEqualTo(HttpStatus.OK);
        assertThat(anonBody).contains("\"name\":\"" + name + "\"");
    }

    @Dann("wird die Personenliste ohne Anmeldung abgewiesen")
    public void anonymousListRejected() {
        assertThat(anonStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Autowired
    private finance.PersonRepository personRepository;

    @Angenommen("die Person {string} ohne App-Nutzer existiert")
    public void personWithoutUser(String name) {
        personRepository.save(new finance.PersonEntity(name));
    }

    @Angenommen("noch keine Person angelegt wurde")
    public void noPersonCreatedYet() {
    }

    @Angenommen("die Person {string} existiert")
    public void personExists(String name) {
        createPerson(name);
    }

    @Wenn("die Person {string} angelegt wird")
    public void createPerson(String name) {
        ResponseEntity<String> response = post(name);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("die Person {string} noch einmal angelegt wird")
    public void createPersonAgain(String name) {
        ResponseEntity<String> response = post(name);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("die Person ohne Name angelegt werden soll")
    public void createPersonWithoutName() {
        ResponseEntity<String> response = post("");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert genau die Person {string}")
    public void exactlyThisPerson(String name) {
        JsonNode persons = queryAll();
        assertThat(persons.size()).isEqualTo(1);
        assertThat(persons.get(0).path("name").asText()).isEqualTo(name);
    }

    @Dann("existiert keine Person")
    public void noPerson() {
        assertThat(queryAll().size()).isZero();
    }

    private ResponseEntity<String> post(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        body.put("passwort", AuthSteps.TEST_PASSWORD);
        return rest.postForEntity("/api/personen", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode queryAll() {
        ResponseEntity<String> list = rest.getForEntity("/api/personen", String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(list.getBody());
    }
}
