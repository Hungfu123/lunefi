package finance;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AusgleichServiceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final Clock UHR = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    private EintragRepository eintragRepository;
    private PersonRepository personRepository;
    private KategorieRepository kategorieRepository;
    private AusgleichsRepository ausgleichsRepository;
    private AusgleichService service;

    @BeforeEach
    void aufbauen() {
        eintragRepository = mock(EintragRepository.class);
        personRepository = mock(PersonRepository.class);
        kategorieRepository = mock(KategorieRepository.class);
        ausgleichsRepository = mock(AusgleichsRepository.class);
        service = new AusgleichService(eintragRepository, personRepository, kategorieRepository,
                ausgleichsRepository, UHR);
        when(personRepository.findAll()).thenReturn(List.of(
                new PersonEntity("Alex"),
                new PersonEntity("Kim")));
    }

    @Test
    @DisplayName("Halbhalb: Alex zahlt 60 gemeinsam, Kim schuldet Alex 30")
    void halbhalb() {
        when(eintragRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 1), "-60.00", "Essen", "Alex")));

        AusgleichMitPositionen ausgleich = service.settlement(NOVEMBER);

        assertThat(ausgleich.saldi())
                .containsEntry("Alex", new BigDecimal("30.00"))
                .containsEntry("Kim", new BigDecimal("-30.00"));
        assertThat(ausgleich.vorschlag()).isEqualTo("Kim schuldet Alex 30,00 €");
        assertThat(ausgleich.laeuftNoch()).isTrue();
        assertThat(ausgleich.positionen()).hasSize(1);
        assertThat(ausgleich.positionen().get(0).anteile())
                .containsEntry("Alex", new BigDecimal("30.00"))
                .containsEntry("Kim", new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Eigene Anteile 60/40: Alex zahlt 100 gemeinsam, Kim traegt 40")
    void eigeneAnteile() {
        when(kategorieRepository.findById("Essen")).thenReturn(Optional.of(
                new KategorieEntity("Essen", "ANTEILE", Map.of("Alex", 60, "Kim", 40))));
        when(eintragRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 2), "-100.00", "Essen", "Alex")));

        AusgleichMitPositionen ausgleich = service.settlement(NOVEMBER);

        assertThat(ausgleich.saldi())
                .containsEntry("Alex", new BigDecimal("40.00"))
                .containsEntry("Kim", new BigDecimal("-40.00"));
        assertThat(ausgleich.positionen().get(0).anteile())
                .containsEntry("Alex", new BigDecimal("60.00"))
                .containsEntry("Kim", new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Gemeinsame Einnahme kehrt den Saldo um: Kim erhaelt 30, Alex bekommt 15")
    void gemeinsameEinnahme() {
        when(eintragRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 3), "30.00", "Erstattung", "Kim")));

        AusgleichMitPositionen ausgleich = service.settlement(NOVEMBER);

        assertThat(ausgleich.saldi())
                .containsEntry("Alex", new BigDecimal("15.00"))
                .containsEntry("Kim", new BigDecimal("-15.00"));
    }

    @Test
    @DisplayName("Nur persoenliche Eintraege: kein Ausgleich, kein Vorschlag")
    void nurPersoenliche() {
        when(eintragRepository.findAll()).thenReturn(List.of(
                personal(LocalDate.of(2026, 11, 4), "-42.00", "Essen", "Alex")));

        AusgleichMitPositionen ausgleich = service.settlement(NOVEMBER);

        assertThat(ausgleich.saldi())
                .containsEntry("Alex", new BigDecimal("0.00"))
                .containsEntry("Kim", new BigDecimal("0.00"));
        assertThat(ausgleich.vorschlag()).isNull();
        assertThat(ausgleich.positionen()).isEmpty();
    }

    @Test
    @DisplayName("Eintraege anderer Monate zaehlen nicht")
    void andererMonat() {
        when(eintragRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 10, 31), "-60.00", "Essen", "Alex")));

        AusgleichMitPositionen ausgleich = service.settlement(NOVEMBER);

        assertThat(ausgleich.positionen()).isEmpty();
        assertThat(ausgleich.vorschlag()).isNull();
    }

    @Test
    @DisplayName("Verlauf: ein Eintrag aus Oktober landet im Vormonat mit korrektem Saldo")
    void history() {
        when(eintragRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 10, 20), "-60.00", "Essen", "Alex")));

        List<AusgleichMonat> verlauf = service.history(2);

        assertThat(verlauf).hasSize(2);
        assertThat(verlauf.get(0).monat()).isEqualTo("2026-10");
        assertThat(verlauf.get(0).saldi()).containsEntry("Alex", new BigDecimal("30.00"));
        assertThat(verlauf.get(1).monat()).isEqualTo("2026-11");
        assertThat(verlauf.get(1).laeuftNoch()).isTrue();
    }

    private EintragEntity shared(LocalDate datum, String betrag, String kategorie, String zahler) {
        return new EintragEntity(datum, new BigDecimal(betrag), kategorie, null, true, zahler, null);
    }

    private EintragEntity personal(LocalDate datum, String betrag, String kategorie, String person) {
        return new EintragEntity(datum, new BigDecimal(betrag), kategorie, person, false, null, null);
    }
}
