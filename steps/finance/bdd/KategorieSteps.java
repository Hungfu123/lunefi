package finance.bdd;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class KategorieSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Angenommen("der Aufteilungsschlüssel für Kategorie {string} beträgt")
    public void setSplit(String kategorie, DataTable anteile) {
        ResponseEntity<String> antwort = sendSplit(kategorie, anteile);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Aufteilungsschlüssel für Kategorie {string} auf ungültige Anteile gesetzt werden soll")
    public void setInvalidSplit(String kategorie, DataTable anteile) {
        ResponseEntity<String> antwort = sendSplit(kategorie, anteile);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("der Aufteilungsschlüssel für Kategorie {string} auf Anteile mit einer unbekannten Person gesetzt werden soll")
    public void sharesWithUnknownPerson(String kategorie) {
        HttpHeaders kopf = new HttpHeaders();
        kopf.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "art", "ANTEILE",
                "anteile", Map.of("Alex", 40, "Bla", 60));
        ResponseEntity<String> antwort = rest.exchange(
                "/api/kategorien/" + kategorie + "/split",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(body, kopf),
                String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert keine Kategorie {string}")
    public void noCategory(String kategorie) {
        assertThat(findCategory(kategorie)).isNull();
    }

    @Dann("existiert die Kategorie {string} mit Aufteilung {string}")
    public void categoryWithSplit(String kategorie, String art) {
        JsonNode gefunden = findCategory(kategorie);
        assertThat(gefunden).isNotNull();
        assertThat(gefunden.path("split").path("art").asText()).isEqualTo(art);
        assertThat(gefunden.path("split").path("anteile").isNull()).isTrue();
    }

    private ResponseEntity<String> sendSplit(String kategorie, DataTable anteile) {
        Map<String, Integer> anteileMap = new HashMap<>();
        for (Map<String, String> zeile : anteile.asMaps()) {
            anteileMap.put(zeile.get("Person"), Integer.parseInt(zeile.get("Anteil in Prozent")));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/kategorien/" + kategorie + "/split",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(Map.of("art", "ANTEILE", "anteile", anteileMap), headers),
                String.class);
    }

    private JsonNode findCategory(String kategorie) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/kategorien", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode eintrag : mapper.readTree(antwort.getBody())) {
            if (kategorie.equals(eintrag.path("name").asText())) {
                return eintrag;
            }
        }
        return null;
    }
}
