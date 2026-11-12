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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

    static final String TEST_PASSWORD = "geheim123";
    private static final String ALEX = "Alex";
    private static final String KIM = "Kim";

    private final ObjectMapper mapper = new ObjectMapper();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private finance.AppUserRepository appUserRepository;

    @Autowired
    private Sessions sessions;

    @Autowired
    @Qualifier("unauthenticatedRest")
    private TestRestTemplate unauthenticated;

    private TestRestTemplate loginTemplate;
    private TestRestTemplate accessTemplate;
    private HttpStatusCode loginStatus;
    private HttpStatusCode accessStatus;
    private HttpStatusCode createStatus;

    @Before(order = 2000)
    public void prepareSessions() {
        ensureUser(ALEX, TEST_PASSWORD);
        ensureUser(KIM, TEST_PASSWORD);
        performLogin(sessions.forPerson(ALEX), ALEX, TEST_PASSWORD);
        performLogin(sessions.forPerson(KIM), KIM, TEST_PASSWORD);
    }

    private void ensureUser(String name, String password) {
        finance.AppUserEntity user = appUserRepository.findByUsername(name)
                .orElseGet(() -> new finance.AppUserEntity(name, null));
        user.setPasswordHash(encoder.encode(password));
        appUserRepository.save(user);
    }

    private void performLogin(TestRestTemplate template, String name, String password) {
        template.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", password);
        ResponseEntity<String> response = template.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        template.getForEntity("/api/personen", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Angenommen("die Person {string} mit Passwort {string} eingerichtet wurde")
    public void personWithPasswordSetUp(String name, String password) {
        createStatus = postPerson(sessions.forPerson(ALEX), name, password);
        assertThat(createStatus).isEqualTo(HttpStatus.CREATED);
    }

    @Angenommen("{string} angemeldet ist")
    public void loggedIn(String person) {
        performLogin(sessions.forPerson(person), person, TEST_PASSWORD);
    }

    @Angenommen("noch kein Nutzer eingerichtet ist")
    public void noUserSetUpYet() {
        appUserRepository.deleteAll();
    }

    @Angenommen("die App eingerichtet ist")
    public void appSetUp() {
    }

    @Wenn("sich {string} mit dem Passwort {string} anmeldet")
    public void logIn(String name, String password) {
        loginTemplate = freshTemplate();
        loginTemplate.getForEntity("/api/personen", String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", name);
        body.add("password", password);
        ResponseEntity<String> response = loginTemplate.postForEntity("/api/login",
                new HttpEntity<>(body, headers), String.class);
        loginStatus = response.getStatusCode();
    }

    @Dann("ist die Anmeldung erfolgreich")
    public void loginSuccessful() {
        assertThat(loginStatus).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> response = loginTemplate.getForEntity("/api/eintraege", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird die Anmeldung abgelehnt")
    public void loginRejected() {
        assertThat(loginStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Wenn("jemand ohne Anmeldung die Einträge abruft")
    public void anonymousFetchesEntries() {
        accessTemplate = unauthenticated;
        accessStatus = unauthenticated.getForEntity("/api/eintraege", String.class).getStatusCode();
    }

    @Wenn("sich {string} abmeldet")
    public void logOut(String person) {
        accessTemplate = sessions.forPerson(person);
        ResponseEntity<String> response = accessTemplate.postForEntity("/api/logout",
                HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Dann("wird der Zugriff wegen fehlender Anmeldung abgelehnt")
    public void accessDeniedUnauthenticated() {
        ResponseEntity<String> response = accessTemplate.getForEntity("/api/eintraege", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Wenn("jemand ohne Anmeldung die Person {string} mit dem Passwort {string} anlegt")
    public void anonymousCreatesPerson(String name, String password) {
        unauthenticated.getForEntity("/api/personen", String.class);
        accessTemplate = unauthenticated;
        createStatus = postPerson(unauthenticated, name, password);
    }

    @Wenn("{string} die Person {string} ohne Passwort anlegt")
    public void createPersonWithoutPassword(String caller, String name) {
        createStatus = postPerson(sessions.forPerson(caller), name, null);
    }

    @Wenn("{string} die Person {string} mit dem Passwort {string} anlegt")
    public void createPersonWithPassword(String caller, String name, String password) {
        createStatus = postPerson(sessions.forPerson(caller), name, password);
    }

    @Dann("wird das Anlegen abgelehnt")
    public void creationRejected() {
        assertThat(createStatus).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Dann("wird das Anlegen wegen fehlender Anmeldung abgelehnt")
    public void creationRejectedUnauthenticated() {
        assertThat(createStatus).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Dann("ist die Person {string} eingerichtet")
    public void personSetUp(String name) {
        assertThat(createStatus).isEqualTo(HttpStatus.CREATED);
    }

    @Wenn("{string} die Monatsübersicht {monat} von {string} aufruft")
    public void callerFetchesOverviewOfOther(String caller, YearMonth month, String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/monatsuebersicht/" + month + "?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} den Monatsverlauf von {string} abruft")
    public void callerFetchesHistoryOfOther(String caller, String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/verlauf?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} die Einträge von {string} abfragt")
    public void callerFetchesEntriesOfOther(String caller, String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/eintraege?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} den Kategorie-Verlauf der letzten {int} Monate für {string} für Person {string} abfragt")
    public void callerQueriesCategoryHistoryOfOther(String caller, int months, String category,
            String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/statistik/kategorie/" + category + "?monate=" + months + "&person=" + person,
                String.class).getStatusCode();
    }

    @Wenn("{string} die Monatsprognose von {string} abruft")
    public void callerFetchesForecastOfOther(String caller, String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/prognose?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} die Konten von {string} abruft")
    public void callerFetchesAccountsOfOther(String caller, String person) {
        accessStatus = sessions.forPerson(caller).getForEntity(
                "/api/konten?person=" + person, String.class).getStatusCode();
    }

    @Wenn("{string} ein Konto für {string} anlegt")
    public void callerCreatesAccountForOther(String caller, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "name", "Konto von " + person,
                "startSaldo", new BigDecimal("100.00"),
                "person", person);
        accessStatus = sessions.forPerson(caller).postForEntity("/api/konten",
                new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Dann("wird der Zugriff wegen fehlender Berechtigung abgelehnt")
    public void accessDeniedForbidden() {
        assertThat(accessStatus).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Wenn("{string} eine Ausgabe von {betrag} Euro mit Kategorie {string} für {string} erfasst")
    public void callerRecordsExpenseForOther(String caller, BigDecimal amount, String category, String person) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "datum", LocalDate.of(2026, 11, 15).toString(),
                "betrag", amount,
                "kategorie", category,
                "person", person,
                "gemeinsam", false);
        accessStatus = sessions.forPerson(caller).postForEntity("/api/eintraege",
                new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Wenn("{string} den einzigen Eintrag zu {betrag} Euro ändert")
    public void callerChangesOnlyEntry(String caller, BigDecimal newAmount) {
        TestRestTemplate template = sessions.forPerson(caller);
        JsonNode existing = onlyEntry(template);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        accessStatus = template.exchange("/api/eintraege/" + existing.path("id").asLong(),
                HttpMethod.PUT, new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Wenn("{string} den einzigen Eintrag löscht")
    public void callerDeletesOnlyEntry(String caller) {
        TestRestTemplate template = sessions.forPerson(caller);
        JsonNode existing = onlyEntry(template);
        accessStatus = template.exchange("/api/eintraege/" + existing.path("id").asLong(),
                HttpMethod.DELETE, HttpEntity.EMPTY, String.class).getStatusCode();
    }

    private JsonNode onlyEntry(TestRestTemplate template) {
        ResponseEntity<String> list = template.getForEntity("/api/eintraege", String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode entries = mapper.readTree(list.getBody());
        assertThat(entries.size()).isEqualTo(1);
        return entries.get(0);
    }

    private HttpStatusCode postPerson(TestRestTemplate template, String name, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("passwort", password);
        ResponseEntity<String> response = template.postForEntity("/api/personen",
                new HttpEntity<>(body, headers), String.class);
        return response.getStatusCode();
    }

    private TestRestTemplate freshTemplate() {
        return sessions.template();
    }
}
