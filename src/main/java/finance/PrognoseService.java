package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class PrognoseService {

    static final Set<String> FIXE_KOSTEN_KATEGORIEN =
            Set.of("Miete", "Strom", "Internet", "Telefon", "Versicherung", "Abo");

    private final EintragRepository eintragRepository;
    private final Clock uhr;

    public PrognoseService(EintragRepository eintragRepository, Clock uhr) {
        this.eintragRepository = eintragRepository;
        this.uhr = uhr;
    }

    public Prognose erstelle(List<Eintrag> eintraege, int monate) {
        YearMonth aktuellerMonat = YearMonth.now(uhr);
        String zielmonat = aktuellerMonat.plusMonths(1).toString();

        List<YearMonth> basisMonate = new ArrayList<>();
        for (int zurueck = monate; zurueck >= 1; zurueck--) {
            basisMonate.add(aktuellerMonat.minusMonths(zurueck));
        }

        if (anzahlMonateMitEintraegen(eintraege) < 2) {
            return new Prognose(zielmonat, Map.of(), Map.of(), BigDecimal.ZERO.setScale(2),
                    "WENIG_DATEN", basisMonate.stream().map(YearMonth::toString).toList());
        }

        Map<String, BigDecimal> ausgabenJeKategorie = prognostiziereAusgaben(eintraege, basisMonate);
        Map<String, BigDecimal> einnahmenJeKategorie = prognostiziereEinnahmen(eintraege, basisMonate);

        BigDecimal ausgabenSumme = summe(ausgabenJeKategorie);
        BigDecimal einnahmenSumme = summe(einnahmenJeKategorie);
        BigDecimal differenz = einnahmenSumme.subtract(ausgabenSumme).setScale(2, RoundingMode.HALF_UP);

        return new Prognose(zielmonat, ausgabenJeKategorie, einnahmenJeKategorie, differenz, "OK",
                basisMonate.stream().map(YearMonth::toString).toList());
    }

    private long anzahlMonateMitEintraegen(List<Eintrag> eintraege) {
        return eintraege.stream()
                .map(eintrag -> YearMonth.from(eintrag.datum()))
                .distinct()
                .count();
    }

    private Map<String, BigDecimal> prognostiziereAusgaben(List<Eintrag> eintraege,
            List<YearMonth> basisMonate) {
        Map<String, Map<YearMonth, BigDecimal>> monatssummen = summenJeKategorieUndMonat(eintraege,
                basisMonate, false);

        Map<String, BigDecimal> prognose = new LinkedHashMap<>();
        for (Map.Entry<String, Map<YearMonth, BigDecimal>> kategorie : monatssummen.entrySet()) {
            if (FIXE_KOSTEN_KATEGORIEN.contains(kategorie.getKey())) {
                BigDecimal letzterBekannterWert = letzterBekannterWert(kategorie.getValue(), basisMonate);
                if (letzterBekannterWert != null) {
                    prognose.put(kategorie.getKey(), letzterBekannterWert);
                }
            } else {
                prognose.put(kategorie.getKey(), median(monatssummenMitNullen(kategorie.getValue(),
                        basisMonate)));
            }
        }
        return prognose;
    }

    private BigDecimal letzterBekannterWert(Map<YearMonth, BigDecimal> monatssummen,
            List<YearMonth> basisMonate) {
        for (int i = basisMonate.size() - 1; i >= 0; i--) {
            BigDecimal wert = monatssummen.get(basisMonate.get(i));
            if (wert != null) {
                return wert;
            }
        }
        return null;
    }

    private Map<String, BigDecimal> prognostiziereEinnahmen(List<Eintrag> eintraege,
            List<YearMonth> basisMonate) {
        Map<String, Map<YearMonth, BigDecimal>> monatssummen = summenJeKategorieUndMonat(eintraege,
                basisMonate, true);

        Map<String, BigDecimal> prognose = new LinkedHashMap<>();
        for (Map.Entry<String, Map<YearMonth, BigDecimal>> kategorie : monatssummen.entrySet()) {
            if (!istRegelmaessig(kategorie.getValue())) {
                continue;
            }
            prognose.put(kategorie.getKey(), median(new ArrayList<>(kategorie.getValue().values())));
        }
        return prognose;
    }

    private boolean istRegelmaessig(Map<YearMonth, BigDecimal> monatssummen) {
        if (monatssummen.size() < 2) {
            return false;
        }
        BigDecimal groesster = monatssummen.values().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal kleinster = monatssummen.values().stream()
                .min(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal spannweite = groesster.subtract(kleinster);
        return spannweite.compareTo(groesster.multiply(new BigDecimal("0.5"))) <= 0;
    }

    private Map<String, Map<YearMonth, BigDecimal>> summenJeKategorieUndMonat(List<Eintrag> eintraege,
            List<YearMonth> basisMonate, boolean einnahmen) {
        Map<String, Map<YearMonth, BigDecimal>> summen = new LinkedHashMap<>();
        for (Eintrag eintrag : eintraege) {
            YearMonth monat = YearMonth.from(eintrag.datum());
            if (!basisMonate.contains(monat)) {
                continue;
            }
            if (einnahmen != eintrag.betrag().signum() >= 0) {
                continue;
            }
            summen.computeIfAbsent(eintrag.kategorie(), kategorie -> new LinkedHashMap<>())
                    .merge(monat, eintrag.betrag().abs(), BigDecimal::add);
        }
        return summen;
    }

    private List<BigDecimal> monatssummenMitNullen(Map<YearMonth, BigDecimal> monatssummen,
            List<YearMonth> basisMonate) {
        List<BigDecimal> werte = new ArrayList<>();
        for (YearMonth monat : basisMonate) {
            werte.add(monatssummen.getOrDefault(monat, BigDecimal.ZERO));
        }
        return werte;
    }

    static BigDecimal median(List<BigDecimal> werte) {
        List<BigDecimal> sortiert = new ArrayList<>(werte);
        sortiert.sort(Comparator.naturalOrder());
        int mitte = sortiert.size() / 2;
        if (sortiert.size() % 2 == 1) {
            return sortiert.get(mitte).setScale(2, RoundingMode.HALF_UP);
        }
        return sortiert.get(mitte - 1).add(sortiert.get(mitte))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal summe(Map<String, BigDecimal> werteJeKategorie) {
        BigDecimal summe = BigDecimal.ZERO;
        for (BigDecimal wert : werteJeKategorie.values()) {
            summe = summe.add(wert);
        }
        return summe.setScale(2, RoundingMode.HALF_UP);
    }
}
