package finance.bdd;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Angenommen("noch kein Konto angelegt wurde")
    public void noAccountYet() {
    }

    @Angenommen("das Konto {string} mit Startsaldo {betrag} Euro von {string} existiert")
    public void accountExists(String name, BigDecimal startsaldo, String person) {
        ResponseEntity<String> response = create(name, startsaldo, person);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("das Konto {string} mit Startsaldo {betrag} Euro von {string} angelegt wird")
    public void createAccount(String name, BigDecimal startsaldo, String person) {
        ResponseEntity<String> response = create(name, startsaldo, person);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("die Kontenübersicht aufgerufen wird")
    public void callAccountsOverview() {
        ResponseEntity<String> response = rest.getForEntity("/api/konten", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("zeigt die Kontenübersicht das Konto {string} mit Saldo {betrag} Euro")
    public void accountWithBalance(String name, BigDecimal expectedBalance) {
        JsonNode account = findAccount(name);
        assertThat(account).isNotNull();
        assertThat(account.path("saldo").decimalValue()).isEqualByComparingTo(expectedBalance);
    }

    @Dann("beträgt der Gesamtsaldo von {string} {betrag} Euro")
    public void totalBalance(String person, BigDecimal expectedBalance) {
        ResponseEntity<String> response = rest.getForEntity("/api/konten?person=" + person, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode accounts = mapper.readTree(response.getBody());
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode account : accounts) {
            sum = sum.add(account.path("saldo").decimalValue());
        }
        assertThat(sum).isEqualByComparingTo(expectedBalance);
    }

    private ResponseEntity<String> create(String name, BigDecimal startsaldo, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "name", name,
                "startSaldo", startsaldo,
                "person", person);
        return rest.postForEntity("/api/konten", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode findAccount(String name) {
        ResponseEntity<String> response = rest.getForEntity("/api/konten", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode account : mapper.readTree(response.getBody())) {
            if (name.equals(account.path("name").asText())) {
                return account;
            }
        }
        return null;
    }
}
