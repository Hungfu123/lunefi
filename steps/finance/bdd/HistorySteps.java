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

public class HistorySteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    private JsonNode history;

    @Wenn("der Monatsverlauf der letzten {int} Monate abgefragt wird")
    public void queryHistory(int monate) {
        ResponseEntity<String> response = rest.getForEntity("/api/verlauf?monate=" + monate, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        history = mapper.readTree(response.getBody());
    }

    @Wenn("der Monatsverlauf der letzten {int} Monate für Person {string} abgefragt wird")
    public void queryHistoryForPerson(int monate, String person) {
        ResponseEntity<String> response = sessions.forPerson(person).getForEntity(
                "/api/verlauf?monate=" + monate + "&person=" + person, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        history = mapper.readTree(response.getBody());
    }

    @Wenn("der Monatsverlauf der letzten {int} Monate in der Paar-Sicht abgefragt wird")
    public void queryHistoryForCouple(int monate) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/verlauf?monate=" + monate + "&gemeinsam=true", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        history = mapper.readTree(response.getBody());
    }

    @Dann("enthält der Monatsverlauf die Monate")
    public void historyContains(DataTable expected) {
        List<Map<String, String>> rows = expected.asMaps();
        assertThat(history.size()).isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            assertThat(history.get(i).path("monat").asText()).isEqualTo(rows.get(i).get("Monat"));
            assertThat(history.get(i).path("einnahmen").decimalValue())
                    .isEqualByComparingTo(parseAmount(rows.get(i).get("Einnahmen")));
            assertThat(history.get(i).path("ausgaben").decimalValue())
                    .isEqualByComparingTo(parseAmount(rows.get(i).get("Ausgaben")));
            assertThat(history.get(i).path("differenz").decimalValue())
                    .isEqualByComparingTo(parseAmount(rows.get(i).get("Differenz")));
        }
    }

    private BigDecimal parseAmount(String rawValue) {
        return new BigDecimal(rawValue.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }
}
