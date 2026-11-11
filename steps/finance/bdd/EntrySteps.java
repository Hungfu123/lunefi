package finance.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EntrySteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Sessions sessions;

    @Autowired
    private finance.EntryRepository entryRepository;

    @Autowired
    private finance.PersonRepository personRepository;

    @Autowired
    private finance.CategoryRepository categoryRepository;

    @Autowired
    private finance.AccountRepository accountRepository;

    @Autowired
    private finance.SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private finance.BudgetRepository budgetRepository;

    @Autowired
    private finance.SourceRepository sourceRepository;

    @Autowired
    private finance.LearningRuleRepository learningRuleRepository;

    @Autowired
    private finance.SettledMonthRepository settledMonthRepository;

    @Before
    public void resetScenario() {
        entryRepository.deleteAll();
        personRepository.deleteAll();
        categoryRepository.deleteAll();
        savingsGoalRepository.deleteAll();
        budgetRepository.deleteAll();
        accountRepository.deleteAll();
        sourceRepository.deleteAll();
        learningRuleRepository.deleteAll();
        settledMonthRepository.deleteAll();
    }

    @ParameterType(value = "\\d{2}\\.\\d{2}\\.\\d{4}", name = "datum")
    public LocalDate parseDate(String rawValue) {
        return LocalDate.parse(rawValue,
                DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT));
    }

    @ParameterType(value = "[+-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2}", name = "betrag")
    public BigDecimal parseAmount(String rawValue) {
        return new BigDecimal(rawValue.replace(".", "").replace(',', '.'));
    }

    @Angenommen("noch kein Eintrag erfasst wurde")
    public void noEntryRecordedYet() {
    }

    @Wenn("ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} von {string} auf dem Konto {string} erfasst wird")
    public void recordEntryOnAccount(LocalDate datum, BigDecimal betrag, String kategorie, String person, String konto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "person", person,
                "gemeinsam", false,
                "konto", konto);
        ResponseEntity<String> response = sessions.forPerson(person).postForEntity("/api/eintraege",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} von {string} erfasst wird")
    public void recordEntry(LocalDate datum, BigDecimal betrag, String kategorie, String person) {
        ResponseEntity<String> response = post(sessions.forPerson(person), Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "person", person,
                "gemeinsam", false));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("{string} eine gemeinsame Ausgabe mit {betrag} Euro und Kategorie {string} erfasst")
    public void recordSharedExpense(String zahler, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> response = post(Map.of(
                "datum", LocalDate.of(2026, 11, 15).toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "gemeinsam", true,
                "zahler", zahler));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag ohne Kategorie erfasst werden soll")
    public void recordEntryWithoutCategory() {
        ResponseEntity<String> response = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "person", "Alex",
                "gemeinsam", false));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Datum erfasst werden soll")
    public void recordEntryWithoutDate() {
        ResponseEntity<String> response = post(Map.of(
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Betrag erfasst werden soll")
    public void recordEntryWithoutAmount() {
        ResponseEntity<String> response = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Person und ohne \"gemeinsam\"-Markierung erfasst werden soll")
    public void recordEntryWithoutPersonAndWithoutShared() {
        ResponseEntity<String> response = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag von {string} mit \"gemeinsam\"-Markierung erfasst werden soll")
    public void recordEntryWithPersonAndShared(String person) {
        ResponseEntity<String> response = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", person,
                "gemeinsam", true));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("nur die Einträge von {string} im {monat} abgefragt werden")
    public void queryEntriesOfPerson(String person, YearMonth monat) {
        result = query(Map.of(
                "person", person,
                "monat", monat.toString()), sessions.forPerson(person));
    }

    @Wenn("nur die gemeinsamen Einträge abgefragt werden")
    public void querySharedEntries() {
        result = query(Map.of("gemeinsam", "true"));
    }

    @Wenn("ein gemeinsamer Eintrag von der Person {string} mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} erfasst werden soll")
    public void sharedEntryByPerson(String zahler, LocalDate datum, BigDecimal betrag, String kategorie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "gemeinsam", true,
                "zahler", zahler);
        ResponseEntity<String> response = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert kein Eintrag")
    public void noEntryPresent() {
        assertThat(query(Map.of()).size()).isZero();
    }

    @Dann("existiert genau ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string}")
    public void exactlyOneEntryPresent(LocalDate datum, BigDecimal betrag, String kategorie) {
        JsonNode entries = query(Map.of());
        assertThat(entries.size()).isEqualTo(1);

        JsonNode entry = entries.get(0);
        assertThat(entry.path("datum").asText()).isEqualTo(datum.toString());
        assertThat(entry.path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(entry.path("kategorie").asText()).isEqualTo(kategorie);
    }

    @Dann("gehört der Eintrag {string} und ist nicht gemeinsam")
    public void entryBelongsToPerson(String person) {
        JsonNode entries = query(Map.of());
        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.get(0).path("person").asText()).isEqualTo(person);
        assertThat(entries.get(0).path("gemeinsam").asBoolean()).isFalse();
    }

    @Dann("existiert im Ergebnis genau ein Eintrag mit Betrag {betrag} Euro und Kategorie {string}")
    public void exactlyOneEntryInResult(BigDecimal betrag, String kategorie) {
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(result.get(0).path("kategorie").asText()).isEqualTo(kategorie);
    }

    @Wenn("der Betrag dieses Eintrags zu {betrag} Euro geändert wird")
    public void changeAmountOfThisEntry(BigDecimal newAmount) {
        JsonNode existing = singleEntry();
        Map<String, Object> body = fullBody(existing, newAmount);
        ResponseEntity<String> response = put("/api/eintraege/" + existing.path("id").asLong(), body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Eintrag mit der Nummer {long} geändert werden soll")
    public void changeEntryByNumber(long number) {
        ResponseEntity<String> response = put("/api/eintraege/" + number, Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        changeStatus = response.getStatusCode();
    }

    @Dann("wird das Ändern abgelehnt")
    public void changeRejected() {
        assertThat(changeStatus).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Wenn("dieser Eintrag gelöscht wird")
    public void deleteThisEntry() {
        deletedEntryId = singleEntry().path("id").asLong();
        ResponseEntity<String> response = rest.exchange(
                "/api/eintraege/" + deletedEntryId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Dann("wird ein erneutes Löschen abgelehnt")
    public void repeatedDeleteRejected() {
        ResponseEntity<String> response = rest.exchange(
                "/api/eintraege/" + deletedEntryId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private org.springframework.http.HttpStatusCode changeStatus;

    private long deletedEntryId;

    private JsonNode singleEntry() {
        JsonNode entries = query(Map.of());
        assertThat(entries.size()).isEqualTo(1);
        return entries.get(0);
    }

    private Map<String, Object> fullBody(JsonNode existing, BigDecimal newAmount) {
        Map<String, Object> body = new HashMap<>();
        body.put("datum", existing.path("datum").asText());
        body.put("betrag", newAmount);
        body.put("kategorie", existing.path("kategorie").asText());
        if (existing.path("gemeinsam").asBoolean()) {
            body.put("gemeinsam", true);
            body.put("zahler", existing.path("zahler").asText());
        } else {
            body.put("person", existing.path("person").asText());
            body.put("gemeinsam", false);
        }
        if (existing.hasNonNull("konto")) {
            body.put("konto", existing.path("konto").asText());
        }
        return body;
    }

    private ResponseEntity<String> put(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode result;

    private JsonNode query(Map<String, String> filter) {
        return query(filter, rest);
    }

    private JsonNode query(Map<String, String> filter, TestRestTemplate template) {
        StringBuilder url = new StringBuilder("/api/eintraege");
        if (!filter.isEmpty()) {
            url.append('?');
            filter.forEach((key, value) -> url.append(key).append('=').append(value).append('&'));
            url.deleteCharAt(url.length() - 1);
        }
        ResponseEntity<String> list = template.getForEntity(url.toString(), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(list.getBody());
    }

    private ResponseEntity<String> post(Map<String, Object> body) {
        return post(rest, body);
    }

    private ResponseEntity<String> post(TestRestTemplate template, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return template.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
    }
}
