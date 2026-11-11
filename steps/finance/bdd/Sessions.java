package finance.bdd;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

public class Sessions {

    private static final String KIM = "Kim";

    private final ApplicationContext context;
    private final RestTemplateBuilder builder;
    private TestRestTemplate alex;
    private TestRestTemplate kim;

    public Sessions(ApplicationContext context, RestTemplateBuilder builder) {
        this.context = context;
        this.builder = builder;
    }

    public TestRestTemplate forPerson(String person) {
        if (KIM.equals(person)) {
            if (kim == null) {
                kim = template();
            }
            return kim;
        }
        if (alex == null) {
            alex = template();
        }
        return alex;
    }

    public TestRestTemplate template() {
        org.springframework.boot.test.http.server.LocalTestWebServer server =
                org.springframework.boot.test.http.server.LocalTestWebServer.obtain(context);
        java.util.List<TestRestTemplate.HttpClientOption> options = new java.util.ArrayList<>();
        options.add(TestRestTemplate.HttpClientOption.ENABLE_COOKIES);
        if (server.scheme() == org.springframework.boot.test.http.server.LocalTestWebServer.Scheme.HTTPS) {
            options.add(TestRestTemplate.HttpClientOption.SSL);
        }
        TestRestTemplate template = new TestRestTemplate(builder, null, null,
                options.toArray(new TestRestTemplate.HttpClientOption[0]));
        template.setUriTemplateHandler(server.uriBuilderFactory());
        template.getRestTemplate().getInterceptors().add(new XsrfHeaderInterceptor());
        return template;
    }
}
