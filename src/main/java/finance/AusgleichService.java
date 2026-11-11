package finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AusgleichService {

    private final EintragRepository eintragRepository;
    private final PersonRepository personRepository;
    private final KategorieRepository kategorieRepository;
    private final AusgleichsRepository ausgleichsRepository;
    private final java.time.Clock uhr;

    public AusgleichService(EintragRepository eintragRepository, PersonRepository personRepository,
            KategorieRepository kategorieRepository, AusgleichsRepository ausgleichsRepository,
            java.time.Clock uhr) {
        this.eintragRepository = eintragRepository;
        this.personRepository = personRepository;
        this.kategorieRepository = kategorieRepository;
        this.ausgleichsRepository = ausgleichsRepository;
        this.uhr = uhr;
    }

    public AusgleichMitPositionen settlement(YearMonth monat) {
        List<String> personen = personRepository.findAll().stream()
                .map(PersonEntity::getName)
                .sorted()
                .toList();

        Map<String, BigDecimal> saldi = new LinkedHashMap<>();
        for (String person : personen) {
            saldi.put(person, BigDecimal.ZERO.setScale(2));
        }

        List<EintragEntity> gemeinsame = eintragRepository.findAll().stream()
                .filter(eintrag -> Boolean.TRUE.equals(eintrag.getGemeinsam()))
                .filter(eintrag -> YearMonth.from(eintrag.getDatum()).equals(monat))
                .sorted(Comparator.comparing(EintragEntity::getDatum)
                        .thenComparing(EintragEntity::getId))
                .toList();

        List<AusgleichPosition> positionen = new ArrayList<>();
        for (EintragEntity eintrag : gemeinsame) {
            Map<String, BigDecimal> anteile = sharesFor(eintrag, personen);
            positionen.add(new AusgleichPosition(eintrag.getId(), eintrag.getKategorie(),
                    eintrag.getBetrag(), eintrag.getZahler(), anteile));

            BigDecimal summe = anteile.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            int vorzeichen = eintrag.getBetrag().signum() < 0 ? 1 : -1;
            for (Map.Entry<String, BigDecimal> anteil : anteile.entrySet()) {
                BigDecimal verauslagungFuerAndere = anteil.getKey().equals(eintrag.getZahler())
                        ? summe.subtract(anteil.getValue())
                        : anteil.getValue().negate();
                saldi.merge(anteil.getKey(), verauslagungFuerAndere.multiply(
                        BigDecimal.valueOf(vorzeichen)), BigDecimal::add);
            }
        }

        boolean laeuftNoch = monat.equals(YearMonth.now(uhr));
        return new AusgleichMitPositionen(monat.toString(), laeuftNoch,
                ausgleichsRepository.existsByMonat(monat.toString()), saldi,
                vorschlag(saldi), positionen);
    }

    public void markAusgeglichen(YearMonth monat) {
        if (!ausgleichsRepository.existsByMonat(monat.toString())) {
            ausgleichsRepository.save(new AusgleichsEntity(monat.toString(), true));
        }
    }

    public List<AusgleichMonat> history(int monate) {
        YearMonth aktuellerMonat = YearMonth.now(uhr);
        List<AusgleichMonat> verlauf = new ArrayList<>();
        for (int zurueck = monate - 1; zurueck >= 0; zurueck--) {
            AusgleichMitPositionen ausgleich = settlement(aktuellerMonat.minusMonths(zurueck));
            verlauf.add(new AusgleichMonat(ausgleich.monat(), ausgleich.laeuftNoch(),
                    ausgleich.ausgeglichen(), ausgleich.saldi(), ausgleich.vorschlag()));
        }
        return verlauf;
    }

    private Map<String, BigDecimal> sharesFor(EintragEntity eintrag, List<String> personen) {
        BigDecimal summe = eintrag.getBetrag().abs().setScale(2, RoundingMode.HALF_UP);
        Map<String, Integer> prozente = percentagesFor(eintrag.getKategorie(), personen);
        return computeShares(summe, prozente);
    }

    private Map<String, Integer> percentagesFor(String kategorie, List<String> personen) {
        KategorieEntity konfiguriert = kategorieRepository.findById(kategorie).orElse(null);
        if (konfiguriert != null && "ANTEILE".equals(konfiguriert.getArt())
                && !konfiguriert.getAnteile().isEmpty()) {
            return konfiguriert.getAnteile();
        }
        Map<String, Integer> halbhalb = new LinkedHashMap<>();
        int prozent = personen.isEmpty() ? 0 : 100 / personen.size();
        for (String person : personen) {
            halbhalb.put(person, prozent);
        }
        return halbhalb;
    }

    private Map<String, BigDecimal> computeShares(BigDecimal summe, Map<String, Integer> prozente) {
        Map<String, BigDecimal> anteile = new LinkedHashMap<>();
        BigDecimal verteilt = BigDecimal.ZERO.setScale(2);
        Map.Entry<String, Integer> groesster = null;
        for (Map.Entry<String, Integer> prozent : prozente.entrySet()) {
            BigDecimal anteil = summe.multiply(BigDecimal.valueOf(prozent.getValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            anteile.put(prozent.getKey(), anteil);
            verteilt = verteilt.add(anteil);
            if (groesster == null || prozent.getValue() > groesster.getValue()) {
                groesster = prozent;
            }
        }
        BigDecimal rest = summe.subtract(verteilt);
        if (rest.signum() != 0 && groesster != null) {
            anteile.put(groesster.getKey(), anteile.get(groesster.getKey()).add(rest));
        }
        return anteile;
    }

    private String vorschlag(Map<String, BigDecimal> saldi) {
        Map.Entry<String, BigDecimal> groesster = null;
        Map.Entry<String, BigDecimal> kleinster = null;
        for (Map.Entry<String, BigDecimal> saldo : saldi.entrySet()) {
            if (groesster == null || saldo.getValue().compareTo(groesster.getValue()) > 0) {
                groesster = saldo;
            }
            if (kleinster == null || saldo.getValue().compareTo(kleinster.getValue()) < 0) {
                kleinster = saldo;
            }
        }
        if (groesster == null || groesster.getValue().signum() <= 0) {
            return null;
        }
        return kleinster.getKey() + " schuldet " + groesster.getKey() + " "
                + asText(groesster.getValue()) + " €";
    }

    private String asText(BigDecimal betrag) {
        return new java.text.DecimalFormat("#,##0.00",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.GERMANY))
                .format(betrag);
    }
}
