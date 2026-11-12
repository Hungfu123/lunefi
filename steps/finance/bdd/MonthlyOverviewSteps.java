package finance.bdd;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ParameterType;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class MonthlyOverviewSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    private JsonNode overview;

    @ParameterType(value = "[A-Za-zÄÖÜäöüß]+ \\d{4}", name = "monat")
    public YearMonth parseMonth(String rawValue) {
        return YearMonth.parse(rawValue,
                DateTimeFormatter.ofPattern("MMMM uuuu", Locale.GERMANY).withResolverStyle(ResolverStyle.STRICT));
    }

    @Wenn("die Monatsübersicht {monat} aufgerufen wird")
    public void callMonthlyOverview(YearMonth month) {
        ResponseEntity<String> response = rest.getForEntity("/api/monatsuebersicht/" + month, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        overview = mapper.readTree(response.getBody());
    }

    @Wenn("die Monatsübersicht {monat} für Person {string} aufgerufen wird")
    public void callMonthlyOverviewForPerson(YearMonth month, String person) {
        ResponseEntity<String> response = sessions.forPerson(person).getForEntity(
                "/api/monatsuebersicht/" + month + "?person=" + person, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        overview = mapper.readTree(response.getBody());
    }

    @Wenn("die Monatsübersicht {monat} in der Paar-Sicht aufgerufen wird")
    public void callMonthlyOverviewForCouple(YearMonth month) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/monatsuebersicht/" + month + "?gemeinsam=true", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        overview = mapper.readTree(response.getBody());
    }

    @Wenn("{string} die Monatsübersicht {monat} in der Paar-Sicht aufruft")
    public void callMonthlyOverviewForCoupleAs(String caller, YearMonth month) {
        ResponseEntity<String> response = sessions.forPerson(caller).getForEntity(
                "/api/monatsuebersicht/" + month + "?gemeinsam=true", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        overview = mapper.readTree(response.getBody());
    }

    @Wenn("die Monatsübersicht {monat} für Person {string} und in der Paar-Sicht aufgerufen wird")
    public void callMonthlyOverviewForPersonAndCouple(YearMonth month, String person) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/monatsuebersicht/" + month + "?person=" + person + "&gemeinsam=true", String.class);
        responseStatus = response.getStatusCode();
    }

    @Dann("wird die Abfrage abgelehnt")
    public void queryRejected() {
        assertThat(responseStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("die Monatsübersicht Monat {int} im Jahr {int} aufgerufen wird")
    public void callInvalidMonth(int month, int year) {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/monatsuebersicht/" + year + "-" + month, String.class);
        responseStatus = response.getStatusCode();
    }

    @Dann("wird der Monat abgelehnt")
    public void monthRejected() {
        assertThat(responseStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private org.springframework.http.HttpStatusCode responseStatus;

    @Dann("zeigt die Monatsübersicht die Ausgaben je Kategorie")
    public void expensesPerCategory(DataTable expected) {
        verifyCategorySums(expected, "Kategorie", "Ausgaben", "ausgabenJeKategorie");
    }

    @Dann("zeigt die Monatsübersicht die Einnahmen je Kategorie")
    public void incomePerCategory(DataTable expected) {
        verifyCategorySums(expected, "Kategorie", "Einnahmen", "einnahmenJeKategorie");
    }

    @Dann("zählt der Eintrag zu den Einnahmen und nicht zu den Ausgaben")
    public void incomeNotCountedInExpenses() {
        assertThat(overview.path("einnahmenJeKategorie").path("Gehalt").decimalValue())
                .isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(overview.path("ausgabenJeKategorie").size()).isZero();
    }

    @Dann("listet die Monatsübersicht {int} Einträge mit Datum und Betrag")
    public void entriesListed(int count) {
        JsonNode entries = overview.path("eintraege");
        assertThat(entries.size()).isEqualTo(count);
        for (JsonNode entry : entries) {
            assertThat(entry.hasNonNull("datum")).isTrue();
            assertThat(entry.hasNonNull("betrag")).isTrue();
        }
    }

    @Dann("zeigt die Monatsübersicht {monat} bei {string} Ausgaben von {betrag} Euro")
    public void expensesOfCategoryInMonth(YearMonth month, String category, BigDecimal amount) {
        ResponseEntity<String> response = rest.getForEntity("/api/monatsuebersicht/" + month, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode currentOverview = mapper.readTree(response.getBody());
        assertThat(currentOverview.path("ausgabenJeKategorie").path(category).decimalValue())
                .isEqualByComparingTo(amount);
    }

    @Angenommen("das Budget für Kategorie {string} im {monat} auf {betrag} Euro gesetzt wird")
    public void setBudget(String category, YearMonth month, BigDecimal limit) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.ResponseEntity<String> response = rest.exchange(
                "/api/budgets",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(java.util.Map.of(
                        "monat", month.toString(),
                        "kategorie", category,
                        "limit", limit), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("warnt die Monatsübersicht bei Kategorie {string}")
    public void budgetWarning(String category) {
        JsonNode budget = overview.path("budgets").path(category);
        assertThat(budget.path("limit").decimalValue()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(budget.path("ist").decimalValue()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(budget.path("warnung").asBoolean()).isTrue();
    }

    private void verifyCategorySums(DataTable expected, String categoryColumn, String sumColumn, String field) {
        Map<String, BigDecimal> expectedSums = new HashMap<>();
        for (Map<String, String> row : expected.asMaps()) {
            expectedSums.put(row.get(categoryColumn), parseAmount(row.get(sumColumn)));
        }

        JsonNode sums = overview.path(field);
        assertThat(sums.size()).isEqualTo(expectedSums.size());
        for (Map.Entry<String, BigDecimal> expectedSum : expectedSums.entrySet()) {
            assertThat(sums.has(expectedSum.getKey()))
                    .as("Kategorie %s fehlt in %s", expectedSum.getKey(), field)
                    .isTrue();
            assertThat(sums.path(expectedSum.getKey()).decimalValue()).isEqualByComparingTo(expectedSum.getValue());
        }
    }

    private BigDecimal parseAmount(String rawValue) {
        return new BigDecimal(rawValue.replace(" Euro", "").replace(".", "").replace(',', '.'));
    }

    @Dann("zeigt die Monatsübersicht einen Eintrag mit dem Betrag {betrag} Euro")
    public void singleEntryWithAmount(BigDecimal amount) {
        JsonNode entries = overview.path("eintraege");
        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.get(0).path("betrag").decimalValue()).isEqualByComparingTo(amount);
    }

    @Dann("ist das Ist-Budget für {string} {betrag} Euro")
    public void budgetActualForCategory(String category, BigDecimal actual) {
        assertThat(overview.path("budgets").path(category).path("ist").decimalValue())
                .isEqualByComparingTo(actual);
    }
}
