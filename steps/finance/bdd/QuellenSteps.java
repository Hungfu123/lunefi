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

public class QuellenSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private long sourceId(String name) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/quellen", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode quelle : mapper.readTree(antwort.getBody())) {
            if (name.equals(quelle.path("name").asText())) {
                return quelle.path("id").asLong();
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
        ResponseEntity<String> antwort = rest.postForEntity("/api/quellen", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Dann("zeigt die Quellenübersicht die Quelle {string} mit Zustand {string}")
    public void sourceWithState(String name, String zustand) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/quellen", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode gefunden = null;
        for (JsonNode quelle : mapper.readTree(antwort.getBody())) {
            if (name.equals(quelle.path("name").asText())) {
                gefunden = quelle;
            }
        }
        assertThat(gefunden).as("Quelle %s existiert", name).isNotNull();
        assertThat(gefunden.path("zustand").asText()).isEqualTo(zustand);
    }

    @Angenommen("die Lernregel für {string} auf Kategorie {string} gespeichert ist")
    public void saveLearningRule(String kennung, String kategorie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("kennung", kennung, "kategorie", kategorie);
        ResponseEntity<String> antwort = rest.postForEntity("/api/lernregeln", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("der Import der Quelle {string} mit den Umsätzen startet")
    public void startImport(String name, DataTable umsaetze) {
        importEntries(name, umsaetze.asMaps());
    }

    @Angenommen("der Import der Quelle {string} mit dem Umsatz {string} vom {datum} über {betrag} Euro bereits gelaufen ist")
    public void importAlreadyRan(String name, String kennung, java.time.LocalDate datum, BigDecimal betrag) {
        Map<String, String> umsatz = new HashMap<>();
        umsatz.put("externeKennung", kennung);
        umsatz.put("datum", datum.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu", java.util.Locale.GERMANY)));
        umsatz.put("betrag", betrag.toPlainString());
        umsatz.put("verwendungszweck", "Gehalt November");
        importEntries(name, java.util.List.of(umsatz));
    }

    @Wenn("derselbe Import erneut startet")
    public void importAgain() {
        importEntries(stehtAusName, stehtAusZeilen);
    }

    private String stehtAusName;
    private java.util.List<Map<String, String>> stehtAusZeilen;

    private void importEntries(String name, java.util.List<Map<String, String>> zeilen) {
        stehtAusName = name;
        stehtAusZeilen = zeilen;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<Map<String, Object>> umsatzListe = new java.util.ArrayList<>();
        for (Map<String, String> zeile : zeilen) {
            Map<String, Object> umsatz = new HashMap<>();
            umsatz.put("externeKennung", zeile.get("externeKennung"));
            umsatz.put("datum", zeile.get("datum"));
            umsatz.put("betrag", zeile.get("betrag"));
            umsatz.put("verwendungszweck", zeile.get("verwendungszweck"));
            umsatzListe.add(umsatz);
        }
        Map<String, Object> body = Map.of("umsaetze", umsatzListe);
        ResponseEntity<String> antwort = rest.postForEntity(
                "/api/quellen/" + sourceId(name) + "/import",
                new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("existieren genau zwei Einträge im November 2026 von {string}")
    public void exactlyTwoEntries(String person) {
        assertEntries(2, person);
    }

    @Dann("existiert genau ein Eintrag im November 2026 von {string}")
    public void exactlyOneEntry(String person) {
        assertEntries(1, person);
    }

    private void assertEntries(int anzahl, String person) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/eintraege?monat=2026-11&person=" + person, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(antwort.getBody()).size()).isEqualTo(anzahl);
    }

    @Dann("hat der Eintrag vom {datum} über {betrag} Euro die Kategorie {string}")
    public void entryHasCategory(java.time.LocalDate datum, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/eintraege?monat=2026-11", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode eintraege = mapper.readTree(antwort.getBody());
        boolean gefunden = false;
        for (JsonNode eintrag : eintraege) {
            if (datum.toString().equals(eintrag.path("datum").asText())
                    && betrag.compareTo(new BigDecimal(eintrag.path("betrag").asText())) == 0) {
                assertThat(eintrag.path("kategorie").asText()).isEqualTo(kategorie);
                gefunden = true;
            }
        }
        assertThat(gefunden).as("Eintrag vom %s über %s existiert", datum, betrag).isTrue();
    }
}
