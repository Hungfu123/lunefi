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

public class CategorySteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Angenommen("der Aufteilungsschlüssel für Kategorie {string} beträgt")
    public void setSplit(String category, DataTable shares) {
        ResponseEntity<String> response = sendSplit(category, shares);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Aufteilungsschlüssel für Kategorie {string} auf ungültige Anteile gesetzt werden soll")
    public void setInvalidSplit(String category, DataTable shares) {
        ResponseEntity<String> response = sendSplit(category, shares);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("der Aufteilungsschlüssel für Kategorie {string} auf Anteile mit einer unbekannten Person gesetzt werden soll")
    public void sharesWithUnknownPerson(String category) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "art", "ANTEILE",
                "anteile", Map.of("Alex", 40, "Bla", 60));
        ResponseEntity<String> response = rest.exchange(
                "/api/kategorien/" + category + "/split",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert keine Kategorie {string}")
    public void noCategory(String category) {
        assertThat(findCategory(category)).isNull();
    }

    @Dann("existiert die Kategorie {string} mit Aufteilung {string}")
    public void categoryWithSplit(String category, String art) {
        JsonNode found = findCategory(category);
        assertThat(found).isNotNull();
        assertThat(found.path("split").path("art").asText()).isEqualTo(art);
        assertThat(found.path("split").path("anteile").isNull()).isTrue();
    }

    private ResponseEntity<String> sendSplit(String category, DataTable shares) {
        Map<String, Integer> sharesMap = new HashMap<>();
        for (Map<String, String> row : shares.asMaps()) {
            sharesMap.put(row.get("Person"), Integer.parseInt(row.get("Anteil in Prozent")));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/kategorien/" + category + "/split",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(Map.of("art", "ANTEILE", "anteile", sharesMap), headers),
                String.class);
    }

    private JsonNode findCategory(String category) {
        ResponseEntity<String> response = rest.getForEntity("/api/kategorien", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode entry : mapper.readTree(response.getBody())) {
            if (category.equals(entry.path("name").asText())) {
                return entry;
            }
        }
        return null;
    }
}
