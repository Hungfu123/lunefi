package finance;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAuthConfig {

    @Bean
    GoogleTokenVerifier googleTokenVerifier(@Value("${lunefi.google.client-id:}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return credential -> {
                throw new GoogleTokenException("Google-Login ist nicht konfiguriert");
            };
        }
        return new GoogleJwksTokenVerifier(clientId);
    }

    static Map<String, String> parseZuordnung(String raw) {
        Map<String, String> mapping = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return mapping;
        }
        for (String pair : raw.split(",")) {
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq == pair.length() - 1) {
                continue;
            }
            mapping.put(GoogleAuthController.normalize(pair.substring(0, eq).trim()),
                    pair.substring(eq + 1).trim());
        }
        return mapping;
    }
}
