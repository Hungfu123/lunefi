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

public class StatistikSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sitzungen sitzungen;

    private JsonNode kategorieVerlauf;
    private JsonNode sparpotenzial;
    private org.springframework.http.HttpStatusCode abfrageStatus;

    @Wenn("der Kategorie-Verlauf der letzten {int} Monate für {string} abgefragt wird")
    public void queryCategoryHistory(int monate, String kategorie) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/statistik/kategorie/" + kategorie + "?monate=" + monate, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        kategorieVerlauf = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Kategorie-Verlauf der letzten {int} Monate für {string} für Person {string} abgefragt wird")
    public void queryCategoryHistoryForPerson(int monate, String kategorie, String person) {
        ResponseEntity<String> antwort = sitzungen.fuer(person).getForEntity(
                "/api/statistik/kategorie/" + kategorie + "?monate=" + monate + "&person=" + person,
                String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        kategorieVerlauf = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Kategorie-Verlauf für {string} mit {int} Monaten abgefragt wird")
    public void queryCategoryHistoryWithMonths(String kategorie, int monate) {
        abfrageStatus = rest.getForEntity(
                "/api/statistik/kategorie/" + kategorie + "?monate=" + monate, String.class).getStatusCode();
    }

    @Dann("enthält der Kategorie-Verlauf die Monate")
    public void categoryHistoryContains(DataTable erwartet) {
        List<Map<String, String>> zeilen = erwartet.asMaps();
        assertThat(kategorieVerlauf.path("monate").size()).isEqualTo(zeilen.size());
        for (int i = 0; i < zeilen.size(); i++) {
            assertThat(kategorieVerlauf.path("monate").get(i).path("monat").asText())
                    .isEqualTo(zeilen.get(i).get("Monat"));
            assertThat(kategorieVerlauf.path("monate").get(i).path("betrag").decimalValue())
                    .isEqualByComparingTo(parseAmount(zeilen.get(i).get("Betrag")));
        }
    }

    @Dann("ist der Median im Kategorie-Verlauf {betrag} Euro")
    public void categoryHistoryMedianIs(BigDecimal median) {
        assertThat(kategorieVerlauf.path("median").decimalValue()).isEqualByComparingTo(median);
    }

    @Dann("ist der Durchschnitt im Kategorie-Verlauf {betrag} Euro")
    public void categoryHistoryAverageIs(BigDecimal durchschnitt) {
        assertThat(kategorieVerlauf.path("durchschnitt").decimalValue())
                .isEqualByComparingTo(durchschnitt);
    }

    @Dann("wird die Kategorie-Verlauf-Abfrage abgelehnt")
    public void categoryHistoryRejected() {
        assertThat(abfrageStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("das Sparpotenzial für den Monat {monat} abgefragt wird")
    public void querySavingsPotential(java.time.YearMonth monat) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/statistik/sparpotenzial?monat=" + monat, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        sparpotenzial = mapper.readTree(antwort.getBody());
    }

    @Wenn("das Sparpotenzial für den ungültigen Monat {string} abgefragt wird")
    public void querySavingsPotentialWithInvalidMonth(String monat) {
        abfrageStatus = rest.getForEntity(
                "/api/statistik/sparpotenzial?monat=" + monat, String.class).getStatusCode();
    }

    @Dann("enthält das Sparpotenzial genau einen Hinweis für {string} mit Ist {betrag} Euro, üblich {betrag} Euro und dem Text {string}")
    public void savingsPotentialContainsOneHint(String kategorie, BigDecimal ist, BigDecimal ueblich,
            String text) {
        JsonNode hinweise = sparpotenzial.path("hinweise");
        assertThat(hinweise.size()).isEqualTo(1);
        JsonNode hinweis = hinweise.get(0);
        assertThat(hinweis.path("kategorie").asText()).isEqualTo(kategorie);
        assertThat(hinweis.path("ist").decimalValue()).isEqualByComparingTo(ist);
        assertThat(hinweis.path("ueblich").decimalValue()).isEqualByComparingTo(ueblich);
        assertThat(hinweis.path("text").asText()).isEqualTo(text);
    }

    @Dann("enthält das Sparpotenzial keine Hinweise")
    public void savingsPotentialContainsNoHints() {
        assertThat(sparpotenzial.path("hinweise").isEmpty()).isTrue();
    }

    @Dann("ist die Sparpotenzial-Basis {string}")
    public void savingsPotentialBasisIs(String basis) {
        assertThat(sparpotenzial.path("basis").asText()).isEqualTo(basis);
    }

    @Dann("wird die Sparpotenzial-Abfrage abgelehnt")
    public void savingsPotentialRejected() {
        assertThat(abfrageStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private BigDecimal parseAmount(String rohwert) {
        return new BigDecimal(rohwert.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }
}
