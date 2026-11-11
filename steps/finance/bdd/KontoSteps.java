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

public class KontoSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Angenommen("noch kein Konto angelegt wurde")
    public void nochKeinKonto() {
    }

    @Angenommen("das Konto {string} mit Startsaldo {betrag} Euro von {string} existiert")
    public void kontoExistiert(String name, BigDecimal startsaldo, String person) {
        ResponseEntity<String> antwort = anlegen(name, startsaldo, person);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("das Konto {string} mit Startsaldo {betrag} Euro von {string} angelegt wird")
    public void kontoAnlegen(String name, BigDecimal startsaldo, String person) {
        ResponseEntity<String> antwort = anlegen(name, startsaldo, person);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("die Kontenübersicht aufgerufen wird")
    public void kontenuebersichtAufrufen() {
        ResponseEntity<String> antwort = rest.getForEntity("/api/konten", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("zeigt die Kontenübersicht das Konto {string} mit Saldo {betrag} Euro")
    public void kontoMitSaldo(String name, BigDecimal erwarteterSaldo) {
        JsonNode konto = kontenSuchen(name);
        assertThat(konto).isNotNull();
        assertThat(konto.path("saldo").decimalValue()).isEqualByComparingTo(erwarteterSaldo);
    }

    @Dann("beträgt der Gesamtsaldo von {string} {betrag} Euro")
    public void gesamtsaldo(String person, BigDecimal erwarteterSaldo) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/konten?person=" + person, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode konten = mapper.readTree(antwort.getBody());
        BigDecimal summe = BigDecimal.ZERO;
        for (JsonNode konto : konten) {
            summe = summe.add(konto.path("saldo").decimalValue());
        }
        assertThat(summe).isEqualByComparingTo(erwarteterSaldo);
    }

    private ResponseEntity<String> anlegen(String name, BigDecimal startsaldo, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "name", name,
                "startSaldo", startsaldo,
                "person", person);
        return rest.postForEntity("/api/konten", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode kontenSuchen(String name) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/konten", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode konto : mapper.readTree(antwort.getBody())) {
            if (name.equals(konto.path("name").asText())) {
                return konto;
            }
        }
        return null;
    }
}
