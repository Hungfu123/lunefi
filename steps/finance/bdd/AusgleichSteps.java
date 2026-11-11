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

public class AusgleichSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SetzbareUhr uhr;

    private JsonNode ausgleich;
    private JsonNode verlauf;

    @Before
    public void resetClock() {
        uhr.reset();
    }

    @Angenommen("die Personen {string} und {string} existieren")
    public void personsExist(String erste, String zweite) {
        createPerson(erste);
        createPerson(zweite);
    }

    @Angenommen("der aktuelle Monat {monat} ist")
    public void currentMonth(YearMonth monat) {
        uhr.setTo(monat);
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
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/" + monat, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        ausgleich = mapper.readTree(antwort.getBody());
        assertThat(ausgleich.path("ausgeglichen").asBoolean()).isTrue();
    }

    @Dann("zeigt der Ausgleichsverlauf der letzten {int} Monate die Monate")
    public void historyShowsSettledState(int monate, DataTable erwartet) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/verlauf?monate=" + monate, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
        List<Map<String, String>> zeilen = erwartet.asMaps();
        assertThat(verlauf.size()).isEqualTo(zeilen.size());
        for (int i = 0; i < zeilen.size(); i++) {
            assertThat(verlauf.get(i).path("monat").asText()).isEqualTo(zeilen.get(i).get("Monat"));
            assertThat(verlauf.get(i).path("ausgeglichen").asBoolean())
                    .isEqualTo(Boolean.parseBoolean(zeilen.get(i).get("Ausgeglichen")));
        }
    }

    private void markAsSettled(YearMonth monat) {
        ResponseEntity<String> antwort = rest.postForEntity(
                "/api/ausgleich/" + monat + "/ausgeglichen", HttpEntity.EMPTY, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Ausgleich {monat} aufgerufen wird")
    public void callSettlement(YearMonth monat) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/" + monat, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        ausgleich = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Ausgleichsverlauf der letzten {int} Monate abgefragt wird")
    public void queryHistory(int monate) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/verlauf?monate=" + monate, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
    }

    @Dann("schuldet {word} {word} {betrag} Euro")
    public void owes(String schuldner, String glaubiger, BigDecimal betrag) {
        assertThat(ausgleich.path("vorschlag").asText())
                .isEqualTo(schuldner + " schuldet " + glaubiger + " " + asText(betrag) + " €");
        assertThat(ausgleich.path("saldi").path(glaubiger).decimalValue()).isEqualByComparingTo(betrag);
        assertThat(ausgleich.path("saldi").path(schuldner).decimalValue()).isEqualByComparingTo(betrag.negate());
    }

    @Dann("trägt {word} {betrag} Euro und {word} {betrag} Euro davon")
    public void carriesShare(String erstePerson, BigDecimal ersterAnteil,
            String zweitePerson, BigDecimal zweiterAnteil) {
        JsonNode anteile = ausgleich.path("positionen").get(0).path("anteile");
        assertThat(anteile.path(erstePerson).decimalValue()).isEqualByComparingTo(ersterAnteil);
        assertThat(anteile.path(zweitePerson).decimalValue()).isEqualByComparingTo(zweiterAnteil);
    }

    @Dann("ist der Monat als \"läuft noch\" markiert")
    public void laeuftNoch() {
        assertThat(ausgleich.path("laeuftNoch").asBoolean()).isTrue();
    }

    @Dann("enthält der Verlauf die Monate")
    public void historyContains(DataTable erwartet) {
        List<Map<String, String>> zeilen = erwartet.asMaps();
        assertThat(verlauf.size()).isEqualTo(zeilen.size());
        for (int i = 0; i < zeilen.size(); i++) {
            assertThat(verlauf.get(i).path("monat").asText()).isEqualTo(zeilen.get(i).get("Monat"));
            String vorschlag = zeilen.get(i).get("Vorschlag");
            if ("keine".equals(vorschlag)) {
                assertThat(verlauf.get(i).path("vorschlag").isNull()).isTrue();
            } else {
                assertThat(verlauf.get(i).path("vorschlag").asText()).isEqualTo(vorschlag);
            }
        }
    }

    @Dann("zeigt der Verlauf keine einzelnen Ausgaben")
    public void historyWithoutPositions() {
        for (JsonNode eintrag : verlauf) {
            assertThat(eintrag.has("positionen")).isFalse();
        }
    }

    private void createPerson(String name) {
        ResponseEntity<String> antwort = rest.postForEntity("/api/personen",
                new HttpEntity<>(Map.of("name", name), jsonHeader()), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private HttpHeaders jsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String asText(BigDecimal betrag) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return format.format(betrag);
    }
}
