package finance.bdd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class SavingsGoalSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private Long savingsGoalId;

    @Angenommen("noch kein Sparziel existiert")
    public void noSavingsGoalYet() {
    }

    @Angenommen("das Sparziel {string} mit {betrag} Euro existiert")
    public void savingsGoalExists(String name, BigDecimal zielbetrag) {
        ResponseEntity<String> response = post(Map.of(
                "name", name,
                "zielbetrag", zielbetrag));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        savingsGoalId = mapper.readTree(response.getBody()).path("id").asLong();
    }

    @Angenommen("{betrag} Euro eingezahlt wurden")
    public void deposited(BigDecimal amount) {
        deposit(amount, "Alex", LocalDate.of(2026, 11, 10));
    }

    @Wenn("eine Einzahlung von {betrag} Euro durch {string} erfolgt")
    public void depositBy(BigDecimal amount, String person) {
        deposit(amount, person, LocalDate.of(2026, 11, 12));
    }

    @Wenn("das Sparziel {string} mit {betrag} Euro erstellt werden soll")
    public void createSavingsGoalWithoutTargetAmount(String name, BigDecimal zielbetrag) {
        ResponseEntity<String> response = post(Map.of(
                "name", name,
                "zielbetrag", zielbetrag));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("ist das Sparziel erreicht")
    public void savingsGoalReached() {
        JsonNode goals = queryAll();
        assertThat(goals.size()).isEqualTo(1);
        JsonNode goal = goals.get(0);
        assertThat(goal.path("eingezahlt").decimalValue()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(goal.path("erreicht").asBoolean()).isTrue();
        assertThat(goal.path("einzahlungen").size()).isEqualTo(2);
    }

    @Dann("existiert kein Sparziel")
    public void noSavingsGoal() {
        assertThat(queryAll().size()).isZero();
    }

    private void deposit(BigDecimal amount, String person, LocalDate date) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity(
                "/api/sparziele/" + savingsGoalId + "/einzahlungen",
                new HttpEntity<>(Map.of(
                        "betrag", amount,
                        "person", person,
                        "datum", date.toString()), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<String> post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/api/sparziele", new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode queryAll() {
        ResponseEntity<String> list = rest.getForEntity("/api/sparziele", String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(list.getBody());
    }
}
