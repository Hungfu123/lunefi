package finance;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedUser {

    public String name() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    public boolean isAuthenticated() {
        return name() != null;
    }

    public void requireOwnPerson(String person) {
        if (!Objects.equals(name(), person)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
