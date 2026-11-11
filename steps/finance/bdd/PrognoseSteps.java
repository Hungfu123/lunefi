package finance.bdd;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class PrognoseSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private JsonNode prognose;
    private org.springframework.http.HttpStatusCode abfrageStatus;

    @Wenn("die Monatsprognose abgefragt wird")
    public void queryForecast() {
        ResponseEntity<String> antwort = rest.getForEntity("/api/prognose", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        prognose = mapper.readTree(antwort.getBody());
    }

    @Wenn("die Monatsprognose für Person {string} abgefragt wird")
    public void queryForecastForPerson(String person) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/prognose?person=" + person, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        prognose = mapper.readTree(antwort.getBody());
    }

    @Wenn("die Monatsprognose für Person {string} und in der Paar-Sicht abgefragt wird")
    public void queryForecastForPersonAndCouple(String person) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/prognose?person=" + person + "&gemeinsam=true", String.class);
        abfrageStatus = antwort.getStatusCode();
    }

    @Dann("wird die Prognose-Abfrage abgelehnt")
    public void forecastRejected() {
        assertThat(abfrageStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("gilt die Prognose für den Monat {monat}")
    public void forecastIsForMonth(YearMonth monat) {
        assertThat(prognose.path("monat").asText()).isEqualTo(monat.toString());
    }

    @Dann("sind die Basis-Monate {monat}, {monat}, {monat}")
    public void basisMonthsAre(YearMonth erste, YearMonth zweite, YearMonth dritte) {
        List<String> erwartet = Arrays.asList(erste.toString(), zweite.toString(), dritte.toString());
        List<String> tatsaechlich = new ArrayList<>();
        for (JsonNode basisMonat : prognose.path("basisMonate")) {
            tatsaechlich.add(basisMonat.asText());
        }
        assertThat(tatsaechlich).isEqualTo(erwartet);
    }

    @Dann("prognostiziert sie für die Kategorie {string} Ausgaben von {betrag} Euro")
    public void forecastsExpenseForCategory(String kategorie, BigDecimal betrag) {
        assertThat(prognose.path("ausgabenJeKategorie").path(kategorie).decimalValue())
                .isEqualByComparingTo(betrag);
    }

    @Dann("prognostiziert sie für die Kategorie {string} Einnahmen von {betrag} Euro")
    public void forecastsIncomeForCategory(String kategorie, BigDecimal betrag) {
        assertThat(prognose.path("einnahmenJeKategorie").path(kategorie).decimalValue())
                .isEqualByComparingTo(betrag);
    }

    @Dann("enthält sie keine Ausgaben für die Kategorie {string}")
    public void containsNoExpenseForCategory(String kategorie) {
        assertThat(prognose.path("ausgabenJeKategorie").has(kategorie)).isFalse();
    }

    @Dann("enthält sie keine Einnahmen für die Kategorie {string}")
    public void containsNoIncomeForCategory(String kategorie) {
        assertThat(prognose.path("einnahmenJeKategorie").has(kategorie)).isFalse();
    }

    @Dann("beträgt die prognostizierte Differenz {betrag} Euro")
    public void forecastDifferenceIs(BigDecimal differenz) {
        assertThat(prognose.path("differenz").decimalValue()).isEqualByComparingTo(differenz);
    }

    @Dann("ist die Konfidenz {string}")
    public void confidenceIs(String konfidenz) {
        assertThat(prognose.path("konfidenz").asText()).isEqualTo(konfidenz);
    }
}
