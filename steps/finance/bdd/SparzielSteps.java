package finance.bdd;

import java.math.BigDecimal;
import java.time.LocalDate;
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

public class SparzielSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private Long sparzielId;

    @Angenommen("noch kein Sparziel existiert")
    public void nochKeinSparziel() {
    }

    @Angenommen("das Sparziel {string} mit {betrag} Euro existiert")
    public void sparzielExistiert(String name, BigDecimal zielbetrag) {
        ResponseEntity<String> antwort = posten(Map.of(
                "name", name,
                "zielbetrag", zielbetrag));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        sparzielId = mapper.readTree(antwort.getBody()).path("id").asLong();
    }

    @Angenommen("{betrag} Euro eingezahlt wurden")
    public void eingezahlt(BigDecimal betrag) {
        einzahlen(betrag, "Alex", LocalDate.of(2026, 11, 10));
    }

    @Wenn("eine Einzahlung von {betrag} Euro durch {string} erfolgt")
    public void einzahlungDurch(BigDecimal betrag, String person) {
        einzahlen(betrag, person, LocalDate.of(2026, 11, 12));
    }

    @Wenn("das Sparziel {string} mit {betrag} Euro erstellt werden soll")
    public void sparzielOhneZielbetragErstellen(String name, BigDecimal zielbetrag) {
        ResponseEntity<String> antwort = posten(Map.of(
                "name", name,
                "zielbetrag", zielbetrag));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("ist das Sparziel erreicht")
    public void sparzielErreicht() {
        JsonNode sparziele = alleAbfragen();
        assertThat(sparziele.size()).isEqualTo(1);
        JsonNode sparziel = sparziele.get(0);
        assertThat(sparziel.path("eingezahlt").decimalValue()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(sparziel.path("erreicht").asBoolean()).isTrue();
        assertThat(sparziel.path("einzahlungen").size()).isEqualTo(2);
    }

    @Dann("existiert kein Sparziel")
    public void keinSparziel() {
        assertThat(alleAbfragen().size()).isZero();
    }

    private void einzahlen(BigDecimal betrag, String person, LocalDate datum) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> antwort = rest.postForEntity(
                "/api/sparziele/" + sparzielId + "/einzahlungen",
                new HttpEntity<>(Map.of(
                        "betrag", betrag,
                        "person", person,
                        "datum", datum.toString()), headers),
                String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<String> posten(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/api/sparziele", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode alleAbfragen() {
        ResponseEntity<String> liste = rest.getForEntity("/api/sparziele", String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(liste.getBody());
    }
}
