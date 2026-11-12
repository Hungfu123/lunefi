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

public class ForecastSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    private JsonNode forecast;
    private org.springframework.http.HttpStatusCode queryStatus;

    @Wenn("die Monatsprognose abgefragt wird")
    public void queryForecast() {
        ResponseEntity<String> response = rest.getForEntity("/api/prognose", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        forecast = mapper.readTree(response.getBody());
    }

    @Wenn("die Monatsprognose für Person {string} abgefragt wird")
    public void queryForecastForPerson(String person) {
        ResponseEntity<String> response = sessions.forPerson(person).getForEntity(
                "/api/prognose?person=" + person, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        forecast = mapper.readTree(response.getBody());
    }

    @Wenn("die Monatsprognose für Person {string} und in der Paar-Sicht abgefragt wird")
    public void queryForecastForPersonAndCouple(String person) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/prognose?person=" + person + "&gemeinsam=true", String.class);
        queryStatus = response.getStatusCode();
    }

    @Dann("wird die Prognose-Abfrage abgelehnt")
    public void forecastRejected() {
        assertThat(queryStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("gilt die Prognose für den Monat {monat}")
    public void forecastIsForMonth(YearMonth month) {
        assertThat(forecast.path("monat").asText()).isEqualTo(month.toString());
    }

    @Dann("sind die Basis-Monate {monat}, {monat}, {monat}")
    public void basisMonthsAre(YearMonth erste, YearMonth zweite, YearMonth dritte) {
        List<String> expected = Arrays.asList(erste.toString(), zweite.toString(), dritte.toString());
        List<String> actual = new ArrayList<>();
        for (JsonNode baseMonth : forecast.path("basisMonate")) {
            actual.add(baseMonth.asText());
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Dann("prognostiziert sie für die Kategorie {string} Ausgaben von {betrag} Euro")
    public void forecastsExpenseForCategory(String category, BigDecimal amount) {
        assertThat(forecast.path("ausgabenJeKategorie").path(category).decimalValue())
                .isEqualByComparingTo(amount);
    }

    @Dann("prognostiziert sie für die Kategorie {string} Einnahmen von {betrag} Euro")
    public void forecastsIncomeForCategory(String category, BigDecimal amount) {
        assertThat(forecast.path("einnahmenJeKategorie").path(category).decimalValue())
                .isEqualByComparingTo(amount);
    }

    @Dann("enthält sie keine Ausgaben für die Kategorie {string}")
    public void containsNoExpenseForCategory(String category) {
        assertThat(forecast.path("ausgabenJeKategorie").has(category)).isFalse();
    }

    @Dann("enthält sie keine Einnahmen für die Kategorie {string}")
    public void containsNoIncomeForCategory(String category) {
        assertThat(forecast.path("einnahmenJeKategorie").has(category)).isFalse();
    }

    @Dann("beträgt die prognostizierte Differenz {betrag} Euro")
    public void forecastDifferenceIs(BigDecimal differenz) {
        assertThat(forecast.path("differenz").decimalValue()).isEqualByComparingTo(differenz);
    }

    @Dann("ist die Konfidenz {string}")
    public void confidenceIs(String konfidenz) {
        assertThat(forecast.path("konfidenz").asText()).isEqualTo(konfidenz);
    }
}
