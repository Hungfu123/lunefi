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
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EintragSteps {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private finance.EintragSpeicher speicher;

    @Before
    public void szenarioZuruecksetzen() {
        speicher.leeren();
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

    @Wenn("ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string} erfasst wird")
    public void eintragErfassen(LocalDate datum, BigDecimal betrag, String kategorie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", datum.toString(),
                "betrag", betrag,
                "kategorie", kategorie);
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("ein Eintrag ohne Kategorie erfasst werden soll")
    public void eintragOhneKategorieErfassen() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "betrag", new BigDecimal("-8.99"));
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Datum erfasst werden soll")
    public void eintragOhneDatumErfassen() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "betrag", new BigDecimal("-8.99"),
                "kategorie", "Essen");
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Wenn("ein Eintrag ohne Betrag erfasst werden soll")
    public void eintragOhneBetragErfassen() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", LocalDate.of(2026, 11, 16).toString(),
                "kategorie", "Essen");
        ResponseEntity<String> antwort = rest.postForEntity("/api/eintraege", new HttpEntity<>(body, headers), String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("existiert kein Eintrag")
    public void keinEintragVorhanden() {
        ResponseEntity<String> liste = rest.getForEntity("/api/eintraege", String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode eintraege = mapper.readTree(liste.getBody());
        assertThat(eintraege.size()).isZero();
    }

    @Dann("existiert genau ein Eintrag mit Datum {datum}, Betrag {betrag} Euro und Kategorie {string}")
    public void genauEinEintragVorhanden(LocalDate datum, BigDecimal betrag, String kategorie) {
        ResponseEntity<String> liste = rest.getForEntity("/api/eintraege", String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode eintraege = mapper.readTree(liste.getBody());
        assertThat(eintraege.size()).isEqualTo(1);

        JsonNode eintrag = eintraege.get(0);
        assertThat(eintrag.path("datum").asText()).isEqualTo(datum.toString());
        assertThat(eintrag.path("betrag").decimalValue()).isEqualByComparingTo(betrag);
        assertThat(eintrag.path("kategorie").asText()).isEqualTo(kategorie);
    }
}
