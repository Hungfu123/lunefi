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
    public void uhrZuruecksetzen() {
        uhr.zuruecksetzen();
    }

    @Angenommen("die Personen {string} und {string} existieren")
    public void personenExistieren(String erste, String zweite) {
        personAnlegen(erste);
        personAnlegen(zweite);
    }

    @Angenommen("der aktuelle Monat {monat} ist")
    public void aktuellerMonat(YearMonth monat) {
        uhr.auf(monat);
    }

    @Wenn("der Ausgleich {monat} aufgerufen wird")
    public void ausgleichAufrufen(YearMonth monat) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/" + monat, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        ausgleich = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Ausgleichsverlauf der letzten {int} Monate abgefragt wird")
    public void verlaufAbfragen(int monate) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/ausgleich/verlauf?monate=" + monate, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
    }

    @Dann("schuldet {word} {word} {betrag} Euro")
    public void schuldet(String schuldner, String glaubiger, BigDecimal betrag) {
        assertThat(ausgleich.path("vorschlag").asText())
                .isEqualTo(schuldner + " schuldet " + glaubiger + " " + alsText(betrag) + " €");
        assertThat(ausgleich.path("saldi").path(glaubiger).decimalValue()).isEqualByComparingTo(betrag);
        assertThat(ausgleich.path("saldi").path(schuldner).decimalValue()).isEqualByComparingTo(betrag.negate());
    }

    @Dann("trägt {word} {betrag} Euro und {word} {betrag} Euro davon")
    public void traegtDavon(String erstePerson, BigDecimal ersterAnteil,
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
    public void verlaufEnthaelt(DataTable erwartet) {
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
    public void verlaufOhnePositionen() {
        for (JsonNode eintrag : verlauf) {
            assertThat(eintrag.has("positionen")).isFalse();
        }
    }

    private void personAnlegen(String name) {
        ResponseEntity<String> antwort = rest.postForEntity("/api/personen",
                new HttpEntity<>(Map.of("name", name), jsonHeader()), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private HttpHeaders jsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String alsText(BigDecimal betrag) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return format.format(betrag);
    }
}
