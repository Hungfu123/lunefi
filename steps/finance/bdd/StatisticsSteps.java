package finance.bdd;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class StatisticsSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    private JsonNode categoryHistory;
    private JsonNode savingsPotential;
    private org.springframework.http.HttpStatusCode queryStatus;

    @Wenn("der Kategorie-Verlauf der letzten {int} Monate für {string} abgefragt wird")
    public void queryCategoryHistory(int months, String category) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/statistik/kategorie/" + category + "?monate=" + months, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        categoryHistory = mapper.readTree(response.getBody());
    }

    @Wenn("der Kategorie-Verlauf der letzten {int} Monate für {string} für Person {string} abgefragt wird")
    public void queryCategoryHistoryForPerson(int months, String category, String person) {
        ResponseEntity<String> response = sessions.forPerson(person).getForEntity(
                "/api/statistik/kategorie/" + category + "?monate=" + months + "&person=" + person,
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        categoryHistory = mapper.readTree(response.getBody());
    }

    @Wenn("der Kategorie-Verlauf für {string} mit {int} Monaten abgefragt wird")
    public void queryCategoryHistoryWithMonths(String category, int months) {
        queryStatus = rest.getForEntity(
                "/api/statistik/kategorie/" + category + "?monate=" + months, String.class).getStatusCode();
    }

    @Dann("enthält der Kategorie-Verlauf die Monate")
    public void categoryHistoryContains(DataTable expected) {
        List<Map<String, String>> rows = expected.asMaps();
        assertThat(categoryHistory.path("monate").size()).isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            assertThat(categoryHistory.path("monate").get(i).path("monat").asText())
                    .isEqualTo(rows.get(i).get("Monat"));
            assertThat(categoryHistory.path("monate").get(i).path("betrag").decimalValue())
                    .isEqualByComparingTo(parseAmount(rows.get(i).get("Betrag")));
        }
    }

    @Dann("ist der Median im Kategorie-Verlauf {betrag} Euro")
    public void categoryHistoryMedianIs(BigDecimal median) {
        assertThat(categoryHistory.path("median").decimalValue()).isEqualByComparingTo(median);
    }

    @Dann("ist der Durchschnitt im Kategorie-Verlauf {betrag} Euro")
    public void categoryHistoryAverageIs(BigDecimal durchschnitt) {
        assertThat(categoryHistory.path("durchschnitt").decimalValue())
                .isEqualByComparingTo(durchschnitt);
    }

    @Dann("wird die Kategorie-Verlauf-Abfrage abgelehnt")
    public void categoryHistoryRejected() {
        assertThat(queryStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("das Sparpotenzial für den Monat {monat} abgefragt wird")
    public void querySavingsPotential(java.time.YearMonth month) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/statistik/sparpotenzial?monat=" + month, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        savingsPotential = mapper.readTree(response.getBody());
    }

    @Wenn("das Sparpotenzial für den ungültigen Monat {string} abgefragt wird")
    public void querySavingsPotentialWithInvalidMonth(String month) {
        queryStatus = rest.getForEntity(
                "/api/statistik/sparpotenzial?monat=" + month, String.class).getStatusCode();
    }

    @Dann("enthält das Sparpotenzial genau einen Hinweis für {string} mit Ist {betrag} Euro, üblich {betrag} Euro und dem Text {string}")
    public void savingsPotentialContainsOneHint(String category, BigDecimal actual, BigDecimal ueblich,
            String text) {
        JsonNode hints = savingsPotential.path("hinweise");
        assertThat(hints.size()).isEqualTo(1);
        JsonNode hint = hints.get(0);
        assertThat(hint.path("kategorie").asText()).isEqualTo(category);
        assertThat(hint.path("ist").decimalValue()).isEqualByComparingTo(actual);
        assertThat(hint.path("ueblich").decimalValue()).isEqualByComparingTo(ueblich);
        assertThat(hint.path("text").asText()).isEqualTo(text);
    }

    @Dann("enthält das Sparpotenzial keine Hinweise")
    public void savingsPotentialContainsNoHints() {
        assertThat(savingsPotential.path("hinweise").isEmpty()).isTrue();
    }

    @Dann("ist die Sparpotenzial-Basis {string}")
    public void savingsPotentialBasisIs(String basis) {
        assertThat(savingsPotential.path("basis").asText()).isEqualTo(basis);
    }

    @Dann("wird die Sparpotenzial-Abfrage abgelehnt")
    public void savingsPotentialRejected() {
        assertThat(queryStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private BigDecimal parseAmount(String rawValue) {
        return new BigDecimal(rawValue.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }
}
