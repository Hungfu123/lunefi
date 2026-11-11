package finance.bdd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.cucumber.java.Before;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    static final String TEST_PASSWORT = "geheim123";
    private static final String ALEX = "Alex";
    private static final String KIM = "Kim";

    private final ObjectMapper mapper = new ObjectMapper();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private finance.AppUserRepository appUserRepository;

    @Autowired
    private Sitzungen sitzungen;

    @Autowired
    @Qualifier("unangemeldetRest")
    private TestRestTemplate unangemeldet;

    private TestRestTemplate loginVorlage;
    private TestRestTemplate zugriffVorlage;
    private HttpStatusCode loginStatus;
    private HttpStatusCode zugriffStatus;
    private HttpStatusCode anlegeStatus;

    @Before(order = 2000)
    public void sitzungenVorbereiten() {
        sichererNutzer(ALEX, TEST_PASSWORT);
        sichererNutzer(KIM, TEST_PASSWORT);
        anmelden(sitzungen.fuer(ALEX), ALEX, TEST_PASSWORT);
        anmelden(sitzungen.fuer(KIM), KIM, TEST_PASSWORT);
    }

    private void sichererNutzer(String name, String passwort) {
        appUserRepository.findByUsername(name)
                .orElseGet(() -> appUserRepository.save(new finance.AppUserEntity(name, encoder.encode(passwort))));
    }

    private void anmelden(TestRestTemplate template, String name, String passwort) {
        template.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", passwort);
        ResponseEntity<String> antwort = template.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        template.getForEntity("/api/personen", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Angenommen("die Person {string} mit Passwort {string} eingerichtet wurde")
    public void personWithPasswordSetUp(String name, String passwort) {
        anlegeStatus = postePerson(sitzungen.fuer(ALEX), name, passwort);
        assertThat(anlegeStatus).isEqualTo(HttpStatus.CREATED);
    }

    @Angenommen("{string} angemeldet ist")
    public void loggedIn(String person) {
        anmelden(sitzungen.fuer(person), person, TEST_PASSWORT);
    }

    @Angenommen("noch kein Nutzer eingerichtet ist")
    public void noUserSetUpYet() {
        appUserRepository.deleteAll();
    }

    @Angenommen("die App eingerichtet ist")
    public void appSetUp() {
    }

    @Wenn("sich {string} mit dem Passwort {string} anmeldet")
    public void logIn(String name, String passwort) {
        loginVorlage = frischeVorlage();
        loginVorlage.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", passwort);
        ResponseEntity<String> antwort = loginVorlage.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        loginStatus = antwort.getStatusCode();
    }

    @Dann("ist die Anmeldung erfolgreich")
    public void loginSuccessful() {
        assertThat(loginStatus).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> antwort = loginVorlage.getForEntity("/api/eintraege", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird die Anmeldung abgelehnt")
    public void loginRejected() {
        assertThat(loginStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Wenn("jemand ohne Anmeldung die Einträge abruft")
    public void anonymousFetchesEntries() {
        zugriffVorlage = unangemeldet;
        zugriffStatus = unangemeldet.getForEntity("/api/eintraege", String.class).getStatusCode();
    }

    @Wenn("sich {string} abmeldet")
    public void logOut(String person) {
        zugriffVorlage = sitzungen.fuer(person);
        ResponseEntity<String> antwort = zugriffVorlage.postForEntity("/api/logout",
                HttpEntity.EMPTY, String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird der Zugriff wegen fehlender Anmeldung abgelehnt")
    public void accessDeniedUnauthenticated() {
        ResponseEntity<String> antwort = zugriffVorlage.getForEntity("/api/eintraege", String.class);
        assertThat(antwort.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Wenn("jemand ohne Anmeldung die Person {string} mit dem Passwort {string} anlegt")
    public void anonymousCreatesPerson(String name, String passwort) {
        unangemeldet.getForEntity("/api/personen", String.class);
        zugriffVorlage = unangemeldet;
        anlegeStatus = postePerson(unangemeldet, name, passwort);
    }

    @Wenn("{string} die Person {string} ohne Passwort anlegt")
    public void createPersonWithoutPassword(String aufrufer, String name) {
        anlegeStatus = postePerson(sitzungen.fuer(aufrufer), name, null);
    }

    @Wenn("{string} die Person {string} mit dem Passwort {string} anlegt")
    public void createPersonWithPassword(String aufrufer, String name, String passwort) {
        anlegeStatus = postePerson(sitzungen.fuer(aufrufer), name, passwort);
    }

    @Dann("wird das Anlegen abgelehnt")
    public void creationRejected() {
        assertThat(anlegeStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("wird das Anlegen wegen fehlender Anmeldung abgelehnt")
    public void creationRejectedUnauthenticated() {
        assertThat(anlegeStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Dann("ist die Person {string} eingerichtet")
    public void personSetUp(String name) {
        assertThat(anlegeStatus).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("{string} die Monatsübersicht {monat} von {string} aufruft")
    public void callerFetchesOverviewOfOther(String aufrufer, YearMonth monat, String person) {
        zugriffStatus = sitzungen.fuer(aufrufer).getForEntity(
                "/api/monatsuebersicht/" + monat + "?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} den Monatsverlauf von {string} abruft")
    public void callerFetchesHistoryOfOther(String aufrufer, String person) {
        zugriffStatus = sitzungen.fuer(aufrufer).getForEntity(
                "/api/verlauf?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} die Einträge von {string} abfragt")
    public void callerFetchesEntriesOfOther(String aufrufer, String person) {
        zugriffStatus = sitzungen.fuer(aufrufer).getForEntity(
                "/api/eintraege?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} die Monatsprognose von {string} abruft")
    public void callerFetchesForecastOfOther(String aufrufer, String person) {
        zugriffStatus = sitzungen.fuer(aufrufer).getForEntity(
                "/api/prognose?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} die Konten von {string} abruft")
    public void callerFetchesAccountsOfOther(String aufrufer, String person) {
        zugriffStatus = sitzungen.fuer(aufrufer).getForEntity(
                "/api/konten?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} ein Konto für {string} anlegt")
    public void callerCreatesAccountForOther(String aufrufer, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "name", "Konto von " + person,
                "startSaldo", new BigDecimal("100.00"),
                "person", person);
        zugriffStatus = sitzungen.fuer(aufrufer).postForEntity("/api/konten",
                new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Dann("wird der Zugriff wegen fehlender Berechtigung abgelehnt")
    public void accessDeniedForbidden() {
        assertThat(zugriffStatus).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Wenn("{string} eine Ausgabe von {betrag} Euro mit Kategorie {string} für {string} erfasst")
    public void callerRecordsExpenseForOther(String aufrufer, BigDecimal betrag, String kategorie, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", LocalDate.of(2026, 11, 15).toString(),
                "betrag", betrag,
                "kategorie", kategorie,
                "person", person,
                "gemeinsam", false);
        zugriffStatus = sitzungen.fuer(aufrufer).postForEntity("/api/eintraege",
                new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Wenn("{string} den einzigen Eintrag zu {betrag} Euro ändert")
    public void callerChangesOnlyEntry(String aufrufer, BigDecimal neuerBetrag) {
        TestRestTemplate template = sitzungen.fuer(aufrufer);
        JsonNode vorhandener = einzigerEintrag(template);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        zugriffStatus = template.exchange("/api/eintraege/" + vorhandener.path("id").asLong(),
                HttpMethod.PUT, new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Wenn("{string} den einzigen Eintrag löscht")
    public void callerDeletesOnlyEntry(String aufrufer) {
        TestRestTemplate template = sitzungen.fuer(aufrufer);
        JsonNode vorhandener = einzigerEintrag(template);
        zugriffStatus = template.exchange("/api/eintraege/" + vorhandener.path("id").asLong(),
                HttpMethod.DELETE, HttpEntity.EMPTY, String.class).getStatusCode();
    }

    private JsonNode einzigerEintrag(TestRestTemplate template) {
        ResponseEntity<String> liste = template.getForEntity("/api/eintraege", String.class);
        assertThat(liste.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode eintraege = mapper.readTree(liste.getBody());
        assertThat(eintraege.size()).isEqualTo(1);
        return eintraege.get(0);
    }

    private HttpStatusCode postePerson(TestRestTemplate template, String name, String passwort) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("passwort", passwort);
        ResponseEntity<String> antwort = template.postForEntity("/api/personen",
                new HttpEntity<>(body, headers), String.class);
        return antwort.getStatusCode();
    }

    private TestRestTemplate frischeVorlage() {
        return sitzungen.vorlage();
    }
}
