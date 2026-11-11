package finance.bdd;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class SettlementSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    @Autowired
    private AdjustableClock clock;

    private JsonNode settlement;
    private JsonNode history;

    @Before
    public void resetClock() {
        clock.reset();
    }

    @Angenommen("die Personen {string} und {string} existieren")
    public void personsExist(String first, String second) {
        createPerson(first);
        createPerson(second);
    }

    @Angenommen("der aktuelle Monat {monat} ist")
    public void currentMonth(YearMonth monat) {
        clock.setTo(monat);
    }

    @Angenommen("der Monat {monat} als ausgeglichen markiert wurde")
    public void monthAlreadyMarkedAsSettled(YearMonth monat) {
        markAsSettled(monat);
    }

    @Wenn("der Monat {monat} als ausgeglichen markiert wird")
    @Wenn("der Monat {monat} erneut als ausgeglichen markiert wird")
    public void markMonthAsSettled(YearMonth monat) {
        markAsSettled(monat);
    }

    @Dann("zeigt der Ausgleich {monat} den Monat als ausgeglichen")
    public void monthShownAsSettled(YearMonth monat) {
        ResponseEntity<String> response = rest.getForEntity("/api/ausgleich/" + monat, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        settlement = mapper.readTree(response.getBody());
        assertThat(settlement.path("ausgeglichen").asBoolean()).isTrue();
    }

    @Dann("zeigt der Ausgleichsverlauf der letzten {int} Monate die Monate")
    public void historyShowsSettledState(int monate, DataTable expected) {
        ResponseEntity<String> response = rest.getForEntity("/api/ausgleich/verlauf?monate=" + monate, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        history = mapper.readTree(response.getBody());
        List<Map<String, String>> rows = expected.asMaps();
        assertThat(history.size()).isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            assertThat(history.get(i).path("monat").asText()).isEqualTo(rows.get(i).get("Monat"));
            assertThat(history.get(i).path("ausgeglichen").asBoolean())
                    .isEqualTo(Boolean.parseBoolean(rows.get(i).get("Ausgeglichen")));
        }
    }

    @Wenn("{string} den Ausgleich {monat} als ausgeglichen markiert")
    public void markMonthAsSettledAs(String caller, YearMonth monat) {
        ResponseEntity<String> response = sessions.forPerson(caller).postForEntity(
                "/api/ausgleich/" + monat + "/ausgeglichen", HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void markAsSettled(YearMonth monat) {
        ResponseEntity<String> response = rest.postForEntity(
                "/api/ausgleich/" + monat + "/ausgeglichen", HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Ausgleich {monat} aufgerufen wird")
    public void callSettlement(YearMonth monat) {
        ResponseEntity<String> response = rest.getForEntity("/api/ausgleich/" + monat, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        settlement = mapper.readTree(response.getBody());
    }

    @Wenn("der Ausgleichsverlauf der letzten {int} Monate abgefragt wird")
    public void queryHistory(int monate) {
        ResponseEntity<String> response = rest.getForEntity("/api/ausgleich/verlauf?monate=" + monate, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        history = mapper.readTree(response.getBody());
    }

    @Dann("schuldet {word} {word} {betrag} Euro")
    public void owes(String debtor, String creditor, BigDecimal amount) {
        assertThat(settlement.path("vorschlag").asText())
                .isEqualTo(debtor + " schuldet " + creditor + " " + asText(amount) + " €");
        assertThat(settlement.path("saldi").path(creditor).decimalValue()).isEqualByComparingTo(amount);
        assertThat(settlement.path("saldi").path(debtor).decimalValue()).isEqualByComparingTo(amount.negate());
    }

    @Dann("trägt {word} {betrag} Euro und {word} {betrag} Euro davon")
    public void carriesShare(String firstPerson, BigDecimal firstShare,
            String secondPerson, BigDecimal secondShare) {
        JsonNode shares = settlement.path("positionen").get(0).path("anteile");
        assertThat(shares.path(firstPerson).decimalValue()).isEqualByComparingTo(firstShare);
        assertThat(shares.path(secondPerson).decimalValue()).isEqualByComparingTo(secondShare);
    }

    @Dann("ist der Monat als \"läuft noch\" markiert")
    public void stillRunning() {
        assertThat(settlement.path("laeuftNoch").asBoolean()).isTrue();
    }

    @Dann("enthält der Verlauf die Monate")
    public void historyContains(DataTable expected) {
        List<Map<String, String>> rows = expected.asMaps();
        assertThat(history.size()).isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            assertThat(history.get(i).path("monat").asText()).isEqualTo(rows.get(i).get("Monat"));
            String suggestion = rows.get(i).get("Vorschlag");
            if ("keine".equals(suggestion)) {
                assertThat(history.get(i).path("vorschlag").isNull()).isTrue();
            } else {
                assertThat(history.get(i).path("vorschlag").asText()).isEqualTo(suggestion);
            }
        }
    }

    @Dann("zeigt der Verlauf keine einzelnen Ausgaben")
    public void historyWithoutPositions() {
        for (JsonNode entry : history) {
            assertThat(entry.has("positionen")).isFalse();
        }
    }

    private void createPerson(String name) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        body.put("passwort", AuthSteps.TEST_PASSWORD);
        ResponseEntity<String> response = rest.postForEntity("/api/personen",
                new HttpEntity<>(body, jsonHeader()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private HttpHeaders jsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String asText(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return format.format(amount);
    }
}
