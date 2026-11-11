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

public class VerlaufSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private JsonNode verlauf;

    @Wenn("der Monatsverlauf der letzten {int} Monate abgefragt wird")
    public void queryHistory(int monate) {
        ResponseEntity<String> antwort = rest.getForEntity("/api/verlauf?monate=" + monate, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Monatsverlauf der letzten {int} Monate für Person {string} abgefragt wird")
    public void queryHistoryForPerson(int monate, String person) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/verlauf?monate=" + monate + "&person=" + person, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
    }

    @Wenn("der Monatsverlauf der letzten {int} Monate in der Paar-Sicht abgefragt wird")
    public void queryHistoryForCouple(int monate) {
        ResponseEntity<String> antwort = rest.getForEntity(
                "/api/verlauf?monate=" + monate + "&gemeinsam=true", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
        verlauf = mapper.readTree(antwort.getBody());
    }

    @Dann("enthält der Monatsverlauf die Monate")
    public void historyContains(DataTable erwartet) {
        List<Map<String, String>> zeilen = erwartet.asMaps();
        assertThat(verlauf.size()).isEqualTo(zeilen.size());
        for (int i = 0; i < zeilen.size(); i++) {
            assertThat(verlauf.get(i).path("monat").asText()).isEqualTo(zeilen.get(i).get("Monat"));
            assertThat(verlauf.get(i).path("einnahmen").decimalValue())
                    .isEqualByComparingTo(parseAmount(zeilen.get(i).get("Einnahmen")));
            assertThat(verlauf.get(i).path("ausgaben").decimalValue())
                    .isEqualByComparingTo(parseAmount(zeilen.get(i).get("Ausgaben")));
            assertThat(verlauf.get(i).path("differenz").decimalValue())
                    .isEqualByComparingTo(parseAmount(zeilen.get(i).get("Differenz")));
        }
    }

    private BigDecimal parseAmount(String rohwert) {
        return new BigDecimal(rohwert.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }
}
