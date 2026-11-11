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
    }
}
