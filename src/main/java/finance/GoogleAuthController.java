package finance;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class GoogleAuthController {

    private final GoogleTokenVerifier verifier;
    private final AppUserRepository appUserRepository;
    private final Map<String, String> zuordnung;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public GoogleAuthController(GoogleTokenVerifier verifier, AppUserRepository appUserRepository,
            @Value("${lunefi.google.zuordnung:}") String zuordnung) {
        this.verifier = verifier;
        this.appUserRepository = appUserRepository;
        this.zuordnung = GoogleAuthConfig.parseZuordnung(zuordnung);
    }

    @PostMapping("/api/auth/google")
    public ResponseEntity<Map<String, String>> login(@RequestBody GoogleLoginInput input,
            HttpServletRequest request, HttpServletResponse response) {
        GoogleIdentity identity;
        try {
            identity = verifier.verify(input.credential());
        } catch (GoogleTokenException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = identity.verified()
                ? zuordnung.get(normalize(identity.email()))
                : null;
        if (username == null || appUserRepository.findByUsername(username).isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        securityContextRepository.saveContext(context, request, response);
        SecurityContextHolder.setContext(context);
        return ResponseEntity.ok(Map.of("name", username));
    }

    static String normalize(String email) {
        String lower = email.trim().toLowerCase(Locale.ROOT);
        int at = lower.indexOf('@');
        if (at <= 0) {
            return lower;
        }
        String local = lower.substring(0, at);
        String domain = lower.substring(at);
        if (domain.equals("@googlemail.com")) {
            domain = "@gmail.com";
            local = local.replace(".", "");
        } else if (domain.equals("@gmail.com")) {
            local = local.replace(".", "");
        }
        return local + domain;
    }

    public record GoogleLoginInput(String credential) {
    }
}
