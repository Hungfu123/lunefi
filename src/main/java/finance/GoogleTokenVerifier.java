package finance;

@FunctionalInterface
public interface GoogleTokenVerifier {

    GoogleIdentity verify(String credential);
}
