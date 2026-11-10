package finance;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class EintragSpeicher {

    private final List<Eintrag> eintraege = new ArrayList<>();

    public void hinzufuegen(Eintrag eintrag) {
        eintraege.add(eintrag);
    }

    public List<Eintrag> alle() {
        return List.copyOf(eintraege);
    }

    public void leeren() {
        eintraege.clear();
    }
}
