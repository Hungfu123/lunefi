package finance.bdd;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatusCode;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class CoachingSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    private HttpStatusCode attitudeStatus;
    private JsonNode coaching;

    @Angenommen("die Haltung für Kategorie {string} auf {string} gestellt wird")
    public void setAttitude(String kategorie, String haltung) {
        ResponseEntity<String> response = putAttitude(kategorie, haltung);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("die Haltung für Kategorie {string} auf {string} gestellt werden soll")
    public void trySetAttitude(String kategorie, String haltung) {
        attitudeStatus = putAttitude(kategorie, haltung).getStatusCode();
    }

    @Dann("wird das Einstellen der Haltung abgelehnt")
    public void setAttitudeRejected() {
        assertThat(attitudeStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("zeigt die Kategorien-Liste für {string} die Haltung {string}")
    public void categoryListShowsAttitude(String kategorie, String haltung) {
        assertThat(findCategory(kategorie)).isNotNull();
        assertThat(findCategory(kategorie).path("haltung").asText()).isEqualTo(haltung);
    }

    @Dann("hat die neue Kategorie {string} die Haltung {string}")
    public void newCategoryHasDefaultAttitude(String kategorie, String haltung) {
        categoryListShowsAttitude(kategorie, haltung);
    }

    @Wenn("das Spar-Coaching für {monat} aufgerufen wird")
    public void callCoaching(YearMonth monat) {
        ResponseEntity<String> response = rest.getForEntity("/api/coaching?monat=" + monat, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        coaching = mapper.readTree(response.getBody());
    }

    @Dann("empfiehlt das Coaching bei {string} {betrag} Euro mehr auszugeben")
    public void coachingRecommendsMore(String kategorie, BigDecimal spielraum) {
        JsonNode recommendation = findByCategory(coaching.path("empfehlungen"), kategorie);
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.path("haltung").asText()).isEqualTo("BEWUSST_MEHR");
        BigDecimal limit = recommendation.path("limit").decimalValue();
        BigDecimal ist = recommendation.path("ist").decimalValue();
        assertThat(recommendation.path("spielraum").decimalValue()).isEqualByComparingTo(spielraum);
        assertThat(limit.subtract(ist)).isEqualByComparingTo(spielraum);
        assertThat(recommendation.path("tipp").asString()).isNotBlank();
    }

    @Dann("lobt das Coaching das Sparen bei {string}")
    public void coachingPraisesSaving(String kategorie) {
        JsonNode saved = findByCategory(coaching.path("gutGespart"), kategorie);
        assertThat(saved).isNotNull();
        assertThat(saved.path("haltung").asText()).isEqualTo("SPAREN");
        BigDecimal limit = saved.path("limit").decimalValue();
        BigDecimal ist = saved.path("ist").decimalValue();
        assertThat(ist).isLessThanOrEqualTo(limit);
        assertThat(saved.path("spielraum").decimalValue()).isEqualByComparingTo(limit.subtract(ist).max(BigDecimal.ZERO));
    }

    @Dann("zeigt das Coaching bei {string} nur den verbleibenden Rahmen von {betrag} Euro")
    public void coachingShowsOnlyRemainingBudget(String kategorie, BigDecimal remaining) {
        assertThat(coaching.path("budgetsUebrig").path(kategorie).decimalValue())
                .isEqualByComparingTo(remaining);
        assertThat(findByCategory(coaching.path("empfehlungen"), kategorie)).isNull();
        assertThat(findByCategory(coaching.path("gutGespart"), kategorie)).isNull();
    }

    @Dann("erwähnt das Coaching die Kategorie {string} nirgends")
    public void coachingDoesNotMentionCategory(String kategorie) {
        assertThat(coaching.path("budgetsUebrig").has(kategorie)).isFalse();
        assertThat(findByCategory(coaching.path("empfehlungen"), kategorie)).isNull();
        assertThat(findByCategory(coaching.path("gutGespart"), kategorie)).isNull();
    }

    private ResponseEntity<String> putAttitude(String kategorie, String haltung) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/kategorien/" + kategorie + "/haltung", HttpMethod.PUT,
                new HttpEntity<>(Map.of("haltung", haltung), headers), String.class);
    }

    private JsonNode findByCategory(JsonNode list, String kategorie) {
        for (JsonNode entry : list) {
            if (kategorie.equals(entry.path("kategorie").asText())) {
                return entry;
            }
        }
        return null;
    }

    private JsonNode findCategory(String kategorie) {
        ResponseEntity<String> response = rest.getForEntity("/api/kategorien", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode entry : mapper.readTree(response.getBody())) {
            if (kategorie.equals(entry.path("name").asText())) {
                return entry;
            }
        }
        return null;
    }
}
