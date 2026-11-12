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
    public void setAttitude(String category, String attitude) {
        ResponseEntity<String> response = putAttitude(category, attitude);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("die Haltung für Kategorie {string} auf {string} gestellt werden soll")
    public void trySetAttitude(String category, String attitude) {
        attitudeStatus = putAttitude(category, attitude).getStatusCode();
    }

    @Dann("wird das Einstellen der Haltung abgelehnt")
    public void setAttitudeRejected() {
        assertThat(attitudeStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("zeigt die Kategorien-Liste für {string} die Haltung {string}")
    public void categoryListShowsAttitude(String category, String attitude) {
        assertThat(findCategory(category)).isNotNull();
        assertThat(findCategory(category).path("haltung").asText()).isEqualTo(attitude);
    }

    @Dann("hat die neue Kategorie {string} die Haltung {string}")
    public void newCategoryHasDefaultAttitude(String category, String attitude) {
        categoryListShowsAttitude(category, attitude);
    }

    @Wenn("das Spar-Coaching für {monat} aufgerufen wird")
    public void callCoaching(YearMonth month) {
        ResponseEntity<String> response = rest.getForEntity("/api/coaching?monat=" + month, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        coaching = mapper.readTree(response.getBody());
    }

    @Dann("empfiehlt das Coaching bei {string} {betrag} Euro mehr auszugeben")
    public void coachingRecommendsMore(String category, BigDecimal spielraum) {
        JsonNode recommendation = findByCategory(coaching.path("empfehlungen"), category);
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.path("haltung").asText()).isEqualTo("BEWUSST_MEHR");
        BigDecimal limit = recommendation.path("limit").decimalValue();
        BigDecimal actual = recommendation.path("ist").decimalValue();
        assertThat(recommendation.path("spielraum").decimalValue()).isEqualByComparingTo(spielraum);
        assertThat(limit.subtract(actual)).isEqualByComparingTo(spielraum);
        assertThat(recommendation.path("tipp").asString()).isNotBlank();
    }

    @Dann("lobt das Coaching das Sparen bei {string}")
    public void coachingPraisesSaving(String category) {
        JsonNode saved = findByCategory(coaching.path("gutGespart"), category);
        assertThat(saved).isNotNull();
        assertThat(saved.path("haltung").asText()).isEqualTo("SPAREN");
        BigDecimal limit = saved.path("limit").decimalValue();
        BigDecimal actual = saved.path("ist").decimalValue();
        assertThat(actual).isLessThanOrEqualTo(limit);
        assertThat(saved.path("spielraum").decimalValue()).isEqualByComparingTo(limit.subtract(actual).max(BigDecimal.ZERO));
    }

    @Dann("zeigt das Coaching bei {string} nur den verbleibenden Rahmen von {betrag} Euro")
    public void coachingShowsOnlyRemainingBudget(String category, BigDecimal remaining) {
        assertThat(coaching.path("budgetsUebrig").path(category).decimalValue())
                .isEqualByComparingTo(remaining);
        assertThat(findByCategory(coaching.path("empfehlungen"), category)).isNull();
        assertThat(findByCategory(coaching.path("gutGespart"), category)).isNull();
    }

    @Dann("erwähnt das Coaching die Kategorie {string} nirgends")
    public void coachingDoesNotMentionCategory(String category) {
        assertThat(coaching.path("budgetsUebrig").has(category)).isFalse();
        assertThat(findByCategory(coaching.path("empfehlungen"), category)).isNull();
        assertThat(findByCategory(coaching.path("gutGespart"), category)).isNull();
    }

    private ResponseEntity<String> putAttitude(String category, String attitude) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/kategorien/" + category + "/haltung", HttpMethod.PUT,
                new HttpEntity<>(Map.of("haltung", attitude), headers), String.class);
    }

    private JsonNode findByCategory(JsonNode list, String category) {
        for (JsonNode entry : list) {
            if (category.equals(entry.path("kategorie").asText())) {
                return entry;
            }
        }
        return null;
    }

    private JsonNode findCategory(String category) {
        ResponseEntity<String> response = rest.getForEntity("/api/kategorien", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode entry : mapper.readTree(response.getBody())) {
            if (category.equals(entry.path("name").asText())) {
                return entry;
            }
        }
        return null;
    }
}
