package finance;

import java.net.URI;
import java.text.ParseException;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * Prüft ein Google-ID-Token wie in der Google-Doku für "Login by Google":
 * Signatur gegen Googles öffentliche Schlüssel (JWKS), Aussteller und
 * Zuhörer (unsere Client-ID) werden kontrolliert, das Ablaufdatum prüft der
 * Prozessor selbst. Zurück bleiben Mail-Adresse und deren Verifiziert-Flag.
 */
final class GoogleJwksTokenVerifier implements GoogleTokenVerifier {

    private static final List<String> ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    private final String clientId;

    GoogleJwksTokenVerifier(String clientId) {
        this.clientId = clientId;
        JWKSource<SecurityContext> keySource;
        try {
            keySource = new RemoteJWKSet<>(
                    URI.create("https://www.googleapis.com/oauth2/v3/certs").toURL());
        } catch (java.net.MalformedURLException exception) {
            throw new GoogleTokenException("JWKS-Adresse unlesbar", exception);
        }
        this.processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
    }

    @Override
    public GoogleIdentity verify(String credential) {
        JWTClaimsSet claims;
        try {
            claims = processor.process(credential, null);
        } catch (ParseException | BadJOSEException | com.nimbusds.jose.JOSEException exception) {
            throw new GoogleTokenException("Token ungültig", exception);
        }
        try {
            if (!ISSUERS.contains(claims.getIssuer())) {
                throw new GoogleTokenException("falscher Aussteller");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
                throw new GoogleTokenException("falscher Zuhörer");
            }
            String email = claims.getStringClaim("email");
            Boolean verified = claims.getBooleanClaim("email_verified");
            if (email == null || verified == null) {
                throw new GoogleTokenException("keine Mail-Adresse im Token");
            }
            return new GoogleIdentity(email, verified);
        } catch (ParseException exception) {
            throw new GoogleTokenException("Ansprüche unlesbar", exception);
        }
    }
}
