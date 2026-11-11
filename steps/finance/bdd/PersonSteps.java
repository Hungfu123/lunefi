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

    @Angenommen("noch keine Person angelegt wurde")
    public void nochKeinePersonAngelegt() {
    }

    @Angenommen("die Person {string} existiert")
    public void personExistiert(String name) {
        personAnlegen(name);
    }

    @Wenn("die Person {string} angelegt wird")
    public void personAnlegen(String name) {
        ResponseEntity<String> antwort = posten(name);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("die Person {string} noch einmal angelegt wird")
    public void personNochmalAnlegen(String name) {
        ResponseEntity<String> antwort = posten(name);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("die Person ohne Name angelegt werden soll")
    public void personOhneNameAnlegen() {
        ResponseEntity<String> antwort = posten("");
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert genau die Person {string}")
    public void genauDiesePerson(String name) {
        JsonNode personen = alleAbfragen();
        assertThat(personen.size()).isEqualTo(1);
        assertThat(personen.get(0).path("name").asText()).isEqualTo(name);
    }

    @Dann("existiert keine Person")
    public void keinePerson() {
        assertThat(alleAbfragen().size()).isZero();
    }

    private ResponseEntity<String> posten(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("name", name);
        return rest.postForEntity("/api/personen", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode alleAbfragen() {
        ResponseEntity<String> liste = rest.getForEntity("/api/personen", String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(liste.getBody());
    }
}
