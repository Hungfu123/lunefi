package finance.bdd;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class XsrfHeaderInterceptor implements ClientHttpRequestInterceptor {

    private String token;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        if (token != null && request.getHeaders().getFirst("X-XSRF-TOKEN") == null) {
            request.getHeaders().add("X-XSRF-TOKEN", token);
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getHeaders().get(HttpHeaders.SET_COOKIE) != null) {
            for (String cookie : response.getHeaders().get(HttpHeaders.SET_COOKIE)) {
                if (cookie.startsWith("XSRF-TOKEN=")) {
                    String wert = cookie.substring("XSRF-TOKEN=".length()).split(";", 2)[0];
                    if (!wert.isEmpty()) {
                        token = wert;
                    }
                }
            }
        }
        return response;
    }
}
