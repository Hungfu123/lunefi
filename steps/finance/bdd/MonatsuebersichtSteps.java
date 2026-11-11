package finance.bdd;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ParameterType;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class MonatsuebersichtSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private JsonNode uebersicht;

    @ParameterType("[A-Za-zÄÖÜäöüß]+ \\d{4}")
    public YearMonth monat(String rohwert) {
        return YearMonth.parse(rohwert,
                DateTimeFormatter.ofPattern("MMMM uuuu", Locale.GERMANY).withResolverStyle(ResolverStyle.STRICT));
    }

    @Wenn("die Monatsübersicht {monat} aufgerufen wird")
    public void monatsuebersichtAufrufen(YearMonth monat) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/monatsuebersicht/" + monat, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        uebersicht = mapper.readTree(antwort.getBody());
    }

    @Wenn("die Monatsübersicht Monat {int} im Jahr {int} aufgerufen wird")
    public void ungueltigenMonatAufrufen(int monat, int jahr) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/monatsuebersicht/" + jahr + "-" + monat, String.class);
        antwortStatus = antwort.getStatusCode();
    }

    @Dann("wird der Monat abgelehnt")
    public void monatAbgelehnt() {
        assertThat(antwortStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private org.springframework.http.HttpStatusCode antwortStatus;

    @Dann("zeigt die Monatsübersicht die Ausgaben je Kategorie")
    public void ausgabenJeKategorie(DataTable erwartet) {
        kategorieSummenAbgleichen(erwartet, "Kategorie", "Ausgaben", "ausgabenJeKategorie");
    }

    @Dann("zeigt die Monatsübersicht die Einnahmen je Kategorie")
    public void einnahmenJeKategorie(DataTable erwartet) {
        kategorieSummenAbgleichen(erwartet, "Kategorie", "Einnahmen", "einnahmenJeKategorie");
    }

    @Dann("zählt der Eintrag zu den Einnahmen und nicht zu den Ausgaben")
    public void einnahmeZaehltNichtZuAusgaben() {
        assertThat(uebersicht.path("einnahmenJeKategorie").path("Gehalt").decimalValue())
                .isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(uebersicht.path("ausgabenJeKategorie").size()).isZero();
    }

    @Dann("listet die Monatsübersicht {int} Einträge mit Datum und Betrag")
    public void eintraegeGelistet(int anzahl) {
        JsonNode eintraege = uebersicht.path("eintraege");
        assertThat(eintraege.size()).isEqualTo(anzahl);
        for (JsonNode eintrag : eintraege) {
            assertThat(eintrag.hasNonNull("datum")).isTrue();
            assertThat(eintrag.hasNonNull("betrag")).isTrue();
        }
    }

    @Angenommen("das Budget für Kategorie {string} im {monat} auf {betrag} Euro gesetzt wird")
    public void budgetSetzen(String kategorie, YearMonth monat, BigDecimal limit) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.ResponseEntity<String> antwort = rest.exchange(
                "/api/budgets",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(java.util.Map.of(
                        "monat", monat.toString(),
                        "kategorie", kategorie,
                        "limit", limit), headers),
                String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("warnt die Monatsübersicht bei Kategorie {string}")
    public void budgetWarnung(String kategorie) {
        JsonNode budget = uebersicht.path("budgets").path(kategorie);
        assertThat(budget.path("limit").decimalValue()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(budget.path("ist").decimalValue()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(budget.path("warnung").asBoolean()).isTrue();
    }

    private void kategorieSummenAbgleichen(DataTable erwartet, String kategorieSpalte, String summenSpalte, String feld) {
        Map<String, BigDecimal> erwartete = new HashMap<>();
        for (Map<String, String> zeile : erwartet.asMaps()) {
            erwartete.put(zeile.get(kategorieSpalte), parseBetrag(zeile.get(summenSpalte)));
        }

        JsonNode summen = uebersicht.path(feld);
        assertThat(summen.size()).isEqualTo(erwartete.size());
        for (Map.Entry<String, BigDecimal> erwartet_ : erwartete.entrySet()) {
            assertThat(summen.has(erwartet_.getKey()))
                    .as("Kategorie %s fehlt in %s", erwartet_.getKey(), feld)
                    .isTrue();
            assertThat(summen.path(erwartet_.getKey()).decimalValue()).isEqualByComparingTo(erwartet_.getValue());
        }
    }

    private BigDecimal parseBetrag(String rohwert) {
        return new BigDecimal(rohwert.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }
}
