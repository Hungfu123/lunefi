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

public class EintragSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private finance.EintragRepository eintragRepository;

    @Autowired
    private finance.PersonRepository personRepository;

    @Autowired
    private finance.KategorieRepository kategorieRepository;

    @Autowired
    private finance.KontoRepository kontoRepository;

    @Autowired
    private finance.SparzielRepository sparzielRepository;

    @Autowired
    private finance.BudgetRepository budgetRepository;

    @Autowired
    private finance.QuelleRepository quelleRepository;

    @Autowired
    private finance.LernregelRepository lernregelRepository;

    @Autowired
    private finance.AusgleichsRepository ausgleichsRepository;

    @Before
    public void resetScenario() {
        eintragRepository.deleteAll();
        personRepository.deleteAll();
        kategorieRepository.deleteAll();
        sparzielRepository.deleteAll();
        budgetRepository.deleteAll();
        kontoRepository.deleteAll();
        quelleRepository.deleteAll();
        lernregelRepository.deleteAll();
        ausgleichsRepository.deleteAll();
    }

    @ParameterType(value = "\\d{2}\\.\\d{2}\\.\\d{4}", name = "datum")
    public LocalDate parseDate(String rohwert) {
        return LocalDate.parse(rohwert,
                DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT));
    }

    @ParameterType(value = "[+-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2}", name = "betrag")
    public BigDecimal parseAmount(String rohwert) {
        return new BigDecimal(rohwert.replace(".", "").replace(',', '.'));
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
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} von {string} erfasst wird")
    public void recordEntry(LocalDate datum, BigDecimal betrag, String kategorie, String person) {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "person", person,
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("{string} eine gemeinsame Ausgabe mit {betrag} Euro und Kategorie {string} erfasst")
    public void recordSharedExpense(String zahler, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", LocalDate.of(2026, 11, 15).toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "gemeinsam", true,
                "zahler", zahler));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag ohne Kategorie erfasst werden soll")
    public void recordEntryWithoutCategory() {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Datum erfasst werden soll")
    public void recordEntryWithoutDate() {
        ResponseEntity<String> antwort = post(Map.of(
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Betrag erfasst werden soll")
    public void recordEntryWithoutAmount() {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Person und ohne \"gemeinsam\"-Markierung erfasst werden soll")
    public void recordEntryWithoutPersonAndWithoutShared() {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen"));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag von {string} mit \"gemeinsam\"-Markierung erfasst werden soll")
    public void recordEntryWithPersonAndShared(String person) {
        ResponseEntity<String> antwort = post(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", person,
                "gemeinsam", true));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("nur die Einträge von {string} im {monat} abgefragt werden")
    public void queryEntriesOfPerson(String person, YearMonth monat) {
        ergebnis = query(Map.of(
                "person", person,
                "monat", monat.toString()));
    }

    @Wenn("nur die gemeinsamen Einträge abgefragt werden")
    public void querySharedEntries() {
        ergebnis = query(Map.of("gemeinsam", "true"));
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
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert kein Eintrag")
    public void noEntryPresent() {
        assertThat(query(Map.of()).size()).isZero();
    }

    @Dann("existiert genau ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string}")
    public void exactlyOneEntryPresent(LocalDate datum, BigDecimal betrag, String kategorie) {
        JsonNode eintraege = query(Map.of());
        assertThat(eintraege.size()).isEqualTo(1);

        JsonNode eintrag = eintraege.get(0);
        assertThat(eintrag.path("datum").asText()).isEqualTo(datum.toString());
        assertThat(eintrag.path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(eintrag.path("kategorie").asText()).isEqualTo(kategorie);
    }

    @Dann("gehört der Eintrag {string} und ist nicht gemeinsam")
    public void entryBelongsToPerson(String person) {
        JsonNode eintraege = query(Map.of());
        assertThat(eintraege.size()).isEqualTo(1);
        assertThat(eintraege.get(0).path("person").asText()).isEqualTo(person);
        assertThat(eintraege.get(0).path("gemeinsam").asBoolean()).isFalse();
    }

    @Dann("existiert im Ergebnis genau ein Eintrag mit Betrag {betrag} Euro und Kategorie {string}")
    public void exactlyOneEntryInResult(BigDecimal betrag, String kategorie) {
        assertThat(ergebnis.size()).isEqualTo(1);
        assertThat(ergebnis.get(0).path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(ergebnis.get(0).path("kategorie").asText()).isEqualTo(kategorie);
    }

    @Wenn("der Betrag dieses Eintrags zu {betrag} Euro geändert wird")
    public void changeAmountOfThisEntry(BigDecimal neuerBetrag) {
        JsonNode vorhandener = singleEntry();
        Map<String, Object> body = fullBody(vorhandener, neuerBetrag);
        ResponseEntity<String> antwort = put("/api/eintraege/" + vorhandener.path("id").asLong(), body);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Wenn("der Eintrag mit der Nummer {long} geändert werden soll")
    public void changeEntryByNumber(long nummer) {
        ResponseEntity<String> antwort = put("/api/eintraege/" + nummer, Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        aenderungStatus = antwort.getStatusCode();
    }

    @Dann("wird das Ändern abgelehnt")
    public void changeRejected() {
        assertThat(aenderungStatus).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Wenn("dieser Eintrag gelöscht wird")
    public void deleteThisEntry() {
        geloeschteEintragId = singleEntry().path("id").asLong();
        ResponseEntity<String> antwort = rest.exchange(
                "/api/eintraege/" + geloeschteEintragId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Dann("wird ein erneutes Löschen abgelehnt")
    public void repeatedDeleteRejected() {
        ResponseEntity<String> antwort = rest.exchange(
                "/api/eintraege/" + geloeschteEintragId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private org.springframework.http.HttpStatusCode aenderungStatus;

    private long geloeschteEintragId;

    private JsonNode singleEntry() {
        JsonNode eintraege = query(Map.of());
        assertThat(eintraege.size()).isEqualTo(1);
        return eintraege.get(0);
    }

    private Map<String, Object> fullBody(JsonNode vorhandener, BigDecimal neuerBetrag) {
        Map<String, Object> body = new HashMap<>();
        body.put("datum", vorhandener.path("datum").asText());
        body.put("betrag", neuerBetrag);
        body.put("kategorie", vorhandener.path("kategorie").asText());
        if (vorhandener.path("gemeinsam").asBoolean()) {
            body.put("gemeinsam", true);
            body.put("zahler", vorhandener.path("zahler").asText());
        } else {
            body.put("person", vorhandener.path("person").asText());
            body.put("gemeinsam", false);
        }
        if (vorhandener.hasNonNull("konto")) {
            body.put("konto", vorhandener.path("konto").asText());
        }
        return body;
    }

    private ResponseEntity<String> put(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode ergebnis;

    private JsonNode query(Map<String, String> filter) {
        StringBuilder url = new StringBuilder("/api/eintraege");
        if (!filter.isEmpty()) {
            url.append('?');
            filter.forEach((schluessel, wert) -> url.append(schluessel).append('=').append(wert).append('&'));
            url.deleteCharAt(url.length() - 1);
        }
        ResponseEntity<String> liste = rest.getForEntity(url.toString(), String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(liste.getBody());
    }

    private ResponseEntity<String> post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
    }
}
