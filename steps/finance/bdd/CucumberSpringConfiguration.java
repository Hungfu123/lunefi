package finance.bdd;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.cucumber.spring.CucumberContextConfiguration;

import finance.GoogleIdentity;
import finance.GoogleTokenException;
import finance.GoogleTokenVerifier;

@Testcontainers
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "lunefi.google.zuordnung=alex@gmail.com=Alex,kim@googlemail.com=Kim")
@AutoConfigureTestRestTemplate
public class CucumberSpringConfiguration {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @TestConfiguration
    static class ClockConfiguration {

        @Bean
        @Primary
        AdjustableClock adjustableClock() {
            return new AdjustableClock();
        }

        @Bean
        Sessions sessions(ApplicationContext context,
                org.springframework.boot.restclient.RestTemplateBuilder builder) {
            return new Sessions(context, builder);
        }

        @Bean
        @Primary
        TestRestTemplate alexRest(Sessions sessions) {
            return sessions.forPerson("Alex");
        }

        @Bean
        TestRestTemplate kimRest(Sessions sessions) {
            return sessions.forPerson("Kim");
        }

        @Bean
        TestRestTemplate unauthenticatedRest(Sessions sessions) {
            return sessions.template();
        }

        @Bean
        @Primary
        GoogleTokenVerifier fakeGoogleVerifier() {
            return credential -> {
                if (credential == null) {
                    throw new GoogleTokenException("leer");
                }
                if (credential.startsWith("google-offen:")) {
                    return new GoogleIdentity(credential.substring("google-offen:".length()), false);
                }
                if (credential.startsWith("google:")) {
                    return new GoogleIdentity(credential.substring("google:".length()), true);
                }
                throw new GoogleTokenException("ungültiges Token");
            };
        }
    }
}
