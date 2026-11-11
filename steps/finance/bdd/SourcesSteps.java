package finance.bdd;

import java.math.BigDecimal;
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

public class SourcesSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private long sourceId(String name) {
        ResponseEntity<String> response = rest.getForEntity("/api/quellen", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode source : mapper.readTree(response.getBody())) {
            if (name.equals(source.path("name").asText())) {
                return source.path("id").asLong();
            }
        }
        throw new AssertionError("Quelle nicht gefunden: " + name);
    }

    @Angenommen("noch keine Quelle angelegt wurde")
    public void noSourceYet() {
    }

    @Angenommen("die Quelle {string} der Art {string} für das Konto {string} existiert")
    public void sourceExists(String name, String art, String konto) {
        createSource(name, art, konto);
    }

    @Wenn("die Quelle {string} der Art {string} für das Konto {string} angelegt wird")
    public void createSource(String name, String art, String konto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("name", name, "art", art, "konto", konto);
        ResponseEntity<String> response = rest.postForEntity("/api/quellen", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Dann("zeigt die Quellenübersicht die Quelle {string} mit Zustand {string}")
    public void sourceWithState(String name, String zustand) {
        ResponseEntity<String> response = rest.getForEntity("/api/quellen", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode found = null;
        for (JsonNode source : mapper.readTree(response.getBody())) {
            if (name.equals(source.path("name").asText())) {
                found = source;
            }
        }
        assertThat(found).as("Quelle %s existiert", name).isNotNull();
        assertThat(found.path("zustand").asText()).isEqualTo(zustand);
    }

    @Angenommen("die Lernregel für {string} auf Kategorie {string} gespeichert ist")
    public void saveLearningRule(String kennung, String kategorie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("kennung", kennung, "kategorie", kategorie);
        ResponseEntity<String> response = rest.postForEntity("/api/lernregeln", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("der Import der Quelle {string} mit den Umsätzen startet")
    public void startImport(String name, DataTable umsaetze) {
        importEntries(name, umsaetze.asMaps());
    }

    @Angenommen("der Import der Quelle {string} mit dem Umsatz {string} vom {datum} über {betrag} Euro bereits gelaufen ist")
    public void importAlreadyRan(String name, String kennung, java.time.LocalDate datum, BigDecimal betrag) {
        Map<String, String> transaction = new HashMap<>();
        transaction.put("externeKennung", kennung);
        transaction.put("datum", datum.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu", java.util.Locale.GERMANY)));
        transaction.put("betrag", betrag.toPlainString());
        transaction.put("verwendungszweck", "Gehalt November");
        importEntries(name, java.util.List.of(transaction));
    }

    @Wenn("derselbe Import erneut startet")
    public void importAgain() {
        importEntries(pendingName, pendingRows);
    }

    private String pendingName;
    private java.util.List<Map<String, String>> pendingRows;

    private void importEntries(String name, java.util.List<Map<String, String>> rows) {
        pendingName = name;
        pendingRows = rows;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<Map<String, Object>> transactionList = new java.util.ArrayList<>();
        for (Map<String, String> row : rows) {
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("externeKennung", row.get("externeKennung"));
            transaction.put("datum", row.get("datum"));
            transaction.put("betrag", row.get("betrag"));
            transaction.put("verwendungszweck", row.get("verwendungszweck"));
            transactionList.add(transaction);
        }
        Map<String, Object> body = Map.of("umsaetze", transactionList);
        ResponseEntity<String> response = rest.postForEntity(
                "/api/quellen/" + sourceId(name) + "/import",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("existieren genau zwei Einträge im November 2026 von {string}")
    public void exactlyTwoEntries(String person) {
        assertEntries(2, person);
    }

    @Dann("existiert genau ein Eintrag im November 2026 von {string}")
    public void exactlyOneEntry(String person) {
        assertEntries(1, person);
    }

    private void assertEntries(int count, String person) {
        ResponseEntity<String> response = rest.getForEntity("/api/eintraege?monat=2026-11&person=" + person, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(response.getBody()).size()).isEqualTo(count);
    }

    @Dann("hat der Eintrag vom {datum} über {betrag} Euro die Kategorie {string}")
    public void entryHasCategory(java.time.LocalDate datum, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> response = rest.getForEntity("/api/eintraege?monat=2026-11", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode entries = mapper.readTree(response.getBody());
        boolean found = false;
        for (JsonNode entry : entries) {
            if (datum.toString().equals(entry.path("datum").asText())
                    && betrag.compareTo(new BigDecimal(entry.path("betrag").asText())) == 0) {
                assertThat(entry.path("kategorie").asText()).isEqualTo(kategorie);
                found = true;
            }
        }
        assertThat(found).as("Eintrag vom %s über %s existiert", datum, betrag).isTrue();
    }
    @Dann("zeigt die Eintragsliste für November 2026 den Verwendungszweck {string}")
    public void entriesContainVerwendungszweck(String verwendungszweck) {
        ResponseEntity<String> response = rest.getForEntity("/api/eintraege?monat=2026-11", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode entries = mapper.readTree(response.getBody());
        boolean found = false;
        for (JsonNode entry : entries) {
            if (verwendungszweck.equals(entry.path("verwendungszweck").asText())) {
                found = true;
            }
        }
        assertThat(found).as("Verwendungszweck %s fehlt in der Eintragsliste", verwendungszweck).isTrue();
    }
}
