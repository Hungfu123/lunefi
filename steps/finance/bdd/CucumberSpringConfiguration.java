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

@Testcontainers
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class CucumberSpringConfiguration {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @TestConfiguration
    static class UhrKonfiguration {

        @Bean
        @Primary
        SetzbareUhr setzbareUhr() {
            return new SetzbareUhr();
        }

        @Bean
        Sitzungen sitzungen(ApplicationContext context,
                org.springframework.boot.restclient.RestTemplateBuilder builder) {
            return new Sitzungen(context, builder);
        }

        @Bean
        @Primary
        TestRestTemplate alexRest(Sitzungen sitzungen) {
            return sitzungen.fuer("Alex");
        }

        @Bean
        TestRestTemplate kimRest(Sitzungen sitzungen) {
            return sitzungen.fuer("Kim");
        }

        @Bean
        TestRestTemplate unangemeldetRest(Sitzungen sitzungen) {
            return sitzungen.vorlage();
        }
    }
}
