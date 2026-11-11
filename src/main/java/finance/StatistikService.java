package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class StatistikService {

    private static final int SPARPOTENZIAL_BASIS_MONATE = 3;
    private static final BigDecimal HINWEIS_SCHWELLE = new BigDecimal("10.00");

    private final Clock uhr;

    public StatistikService(Clock uhr) {
        this.uhr = uhr;
    }

    public StatistikVerlauf verlaufJeKategorie(List<Eintrag> eintraege, String kategorie, int monate) {
        YearMonth aktuellerMonat = YearMonth.now(uhr);

        List<StatistikMonat> verlauf = new ArrayList<>();
        List<BigDecimal> werte = new ArrayList<>();
        BigDecimal summe = BigDecimal.ZERO;
        for (int zurueck = monate - 1; zurueck >= 0; zurueck--) {
            YearMonth monat = aktuellerMonat.minusMonths(zurueck);
            BigDecimal betrag = ausgabenImMonat(eintraege, kategorie, monat);
            werte.add(betrag);
            summe = summe.add(betrag);
            verlauf.add(new StatistikMonat(monat.toString(), betrag));
        }

        BigDecimal median = PrognoseService.median(werte);
        BigDecimal durchschnitt = summe.divide(new BigDecimal(monate), 2, RoundingMode.HALF_UP);
        return new StatistikVerlauf(kategorie, verlauf, median, durchschnitt);
    }

    public Sparpotenzial sparpotenzial(List<Eintrag> eintraege, YearMonth monat) {
        long monateMitEintraegen = eintraege.stream()
                .map(eintrag -> YearMonth.from(eintrag.datum()))
                .filter(vorher -> vorher.isBefore(monat))
                .distinct()
                .count();
        if (monateMitEintraegen < 2) {
            return new Sparpotenzial(List.of(), "WENIG_DATEN");
        }

        List<YearMonth> basisMonate = new ArrayList<>();
        for (int zurueck = SPARPOTENZIAL_BASIS_MONATE; zurueck >= 1; zurueck--) {
            basisMonate.add(monat.minusMonths(zurueck));
        }

        Map<String, BigDecimal> istJeKategorie = ausgabenJeKategorie(eintraege, monat);
        List<SparHinweis> hinweise = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> kategorie : istJeKategorie.entrySet()) {
            if (PrognoseService.FIXE_KOSTEN_KATEGORIEN.contains(kategorie.getKey())) {
                continue;
            }
            BigDecimal ist = kategorie.getValue();
            BigDecimal ueblich = ueblicheAusgaben(eintraege, kategorie.getKey(), basisMonate);
            if (ueblich.signum() <= 0 || ist.compareTo(ueblich) <= 0
                    || ist.subtract(ueblich).compareTo(HINWEIS_SCHWELLE) < 0) {
                continue;
            }
            hinweise.add(new SparHinweis(kategorie.getKey(), ist, ueblich,
                    hinweisText(kategorie.getKey(), ist, ueblich)));
        }
        return new Sparpotenzial(hinweise, "OK");
    }

    private BigDecimal ausgabenImMonat(List<Eintrag> eintraege, String kategorie, YearMonth monat) {
        BigDecimal summe = BigDecimal.ZERO;
        for (Eintrag eintrag : eintraege) {
            if (YearMonth.from(eintrag.datum()).equals(monat)
                    && eintrag.kategorie().equals(kategorie)
                    && eintrag.betrag().signum() < 0) {
                summe = summe.add(eintrag.betrag().abs());
            }
        }
        return summe.setScale(2);
    }

    private Map<String, BigDecimal> ausgabenJeKategorie(List<Eintrag> eintraege, YearMonth monat) {
        Map<String, BigDecimal> summen = new LinkedHashMap<>();
        for (Eintrag eintrag : eintraege) {
            if (!YearMonth.from(eintrag.datum()).equals(monat) || eintrag.betrag().signum() >= 0) {
                continue;
            }
            summen.merge(eintrag.kategorie(), eintrag.betrag().abs(), BigDecimal::add);
        }
        return summen;
    }

    private BigDecimal ueblicheAusgaben(List<Eintrag> eintraege, String kategorie,
            List<YearMonth> basisMonate) {
        List<BigDecimal> werte = new ArrayList<>();
        for (YearMonth basisMonat : basisMonate) {
            werte.add(ausgabenImMonat(eintraege, kategorie, basisMonat));
        }
        return PrognoseService.median(werte);
    }

    private String hinweisText(String kategorie, BigDecimal ist, BigDecimal ueblich) {
        return kategorie + ": " + ganzeEuro(ist) + " € — sonst meist " + ganzeEuro(ueblich) + " €.";
    }

    private String ganzeEuro(BigDecimal betrag) {
        return betrag.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
