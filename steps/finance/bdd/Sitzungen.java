package finance.bdd;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;

public class Sitzungen {

    private static final String KIM = "Kim";

    private final ApplicationContext context;
    private final RestTemplateBuilder builder;
    private TestRestTemplate alex;
    private TestRestTemplate kim;

    public Sitzungen(ApplicationContext context, RestTemplateBuilder builder) {
        this.context = context;
        this.builder = builder;
    }

    public TestRestTemplate fuer(String person) {
        if (KIM.equals(person)) {
            if (kim == null) {
                kim = vorlage();
            }
            return kim;
        }
        if (alex == null) {
            alex = vorlage();
        }
        return alex;
    }

    public TestRestTemplate vorlage() {
        LocalTestWebServer server = LocalTestWebServer.obtain(context);
        List<TestRestTemplate.HttpClientOption> optionen = new ArrayList<>();
        optionen.add(TestRestTemplate.HttpClientOption.ENABLE_COOKIES);
        if (server.scheme() == LocalTestWebServer.Scheme.HTTPS) {
            optionen.add(TestRestTemplate.HttpClientOption.SSL);
        }
        TestRestTemplate template = new TestRestTemplate(builder, null, null,
                optionen.toArray(new TestRestTemplate.HttpClientOption[0]));
        template.setUriTemplateHandler(server.uriBuilderFactory());
        template.getRestTemplate().getInterceptors().add(new XsrfHeaderInterceptor());
        return template;
    }
}
