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
    private finance.SparzielRepository sparzielRepository;

    @Autowired
    private finance.BudgetRepository budgetRepository;

    @Before
    public void szenarioZuruecksetzen() {
        eintragRepository.deleteAll();
        personRepository.deleteAll();
        kategorieRepository.deleteAll();
        sparzielRepository.deleteAll();
        budgetRepository.deleteAll();
    }

    @ParameterType("\\d{2}\\.\\d{2}\\.\\d{4}")
    public LocalDate datum(String rohwert) {
        return LocalDate.parse(rohwert,
                DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT));
    }

    @ParameterType("[+-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2}")
    public BigDecimal betrag(String rohwert) {
        return new BigDecimal(rohwert.replace(".", "").replace(',', '.'));
    }

    @Angenommen("noch kein Eintrag erfasst wurde")
    public void nochKeinEintragErfasst() {
    }

    @Wenn("ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} von {string} erfasst wird")
    public void eintragErfassen(LocalDate datum, BigDecimal betrag, String kategorie, String person) {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "person", person,
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("{string} eine gemeinsame Ausgabe mit {betrag} Euro und Kategorie {string} erfasst")
    public void gemeinsameAusgabeErfassen(String zahler, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", LocalDate.of(2026, 11, 15).toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "gemeinsam", true,
                "zahler", zahler));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag ohne Kategorie erfasst werden soll")
    public void eintragOhneKategorieErfassen() {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Datum erfasst werden soll")
    public void eintragOhneDatumErfassen() {
        ResponseEntity<String> antwort = posten(Map.of(
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Betrag erfasst werden soll")
    public void eintragOhneBetragErfassen() {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "kategorie", "Essen",
                "person", "Alex",
                "gemeinsam", false));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Person und ohne \"gemeinsam\"-Markierung erfasst werden soll")
    public void eintragOhnePersonUndOhneGemeinsamErfassen() {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen"));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag von {string} mit \"gemeinsam\"-Markierung erfasst werden soll")
    public void eintragMitPersonUndGemeinsamErfassen(String person) {
        ResponseEntity<String> antwort = posten(Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen",
                "person", person,
                "gemeinsam", true));
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("nur die Einträge von {string} im {monat} abgefragt werden")
    public void eintraegeEinerPersonAbfragen(String person, YearMonth monat) {
        ergebnis = abfragen(Map.of(
                "person", person,
                "monat", monat.toString()));
    }

    @Wenn("nur die gemeinsamen Einträge abgefragt werden")
    public void gemeinsameEintraegeAbfragen() {
        ergebnis = abfragen(Map.of("gemeinsam", "true"));
    }

    @Wenn("ein gemeinsamer Eintrag von der Person {string} mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} erfasst werden soll")
    public void gemeinsamerEintragVonPerson(String zahler, LocalDate datum, BigDecimal betrag, String kategorie) {
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
    public void keinEintragVorhanden() {
        assertThat(abfragen(Map.of()).size()).isZero();
    }

    @Dann("existiert genau ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string}")
    public void genauEinEintragVorhanden(LocalDate datum, BigDecimal betrag, String kategorie) {
        JsonNode eintraege = abfragen(Map.of());
        assertThat(eintraege.size()).isEqualTo(1);

        JsonNode eintrag = eintraege.get(0);
        assertThat(eintrag.path("datum").asText()).isEqualTo(datum.toString());
        assertThat(eintrag.path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(eintrag.path("kategorie").asText()).isEqualTo(kategorie);
    }

    @Dann("gehört der Eintrag {string} und ist nicht gemeinsam")
    public void eintragGehoertPerson(String person) {
        JsonNode eintraege = abfragen(Map.of());
        assertThat(eintraege.size()).isEqualTo(1);
        assertThat(eintraege.get(0).path("person").asText()).isEqualTo(person);
        assertThat(eintraege.get(0).path("gemeinsam").asBoolean()).isFalse();
    }

    @Dann("existiert im Ergebnis genau ein Eintrag mit Betrag {betrag} Euro und Kategorie {string}")
    public void genauEinEintragImErgebnis(BigDecimal betrag, String kategorie) {
        assertThat(ergebnis.size()).isEqualTo(1);
        assertThat(ergebnis.get(0).path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(ergebnis.get(0).path("kategorie").asText()).isEqualTo(kategorie);
    }

    private JsonNode ergebnis;

    private JsonNode abfragen(Map<String, String> filter) {
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

    private ResponseEntity<String> posten(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
    }
}
