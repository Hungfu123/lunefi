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

class SettlementServiceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    private EntryRepository entryRepository;
    private PersonRepository personRepository;
    private CategoryRepository categoryRepository;
    private SettledMonthRepository settledMonthRepository;
    private EntryPartRepository entryPartRepository;
    private SettlementService service;

    @BeforeEach
    void setUp() {
        entryRepository = mock(EntryRepository.class);
        personRepository = mock(PersonRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        settledMonthRepository = mock(SettledMonthRepository.class);
        entryPartRepository = mock(EntryPartRepository.class);
        when(entryPartRepository.findAllByOrderByEintragIdAscPositionAsc()).thenReturn(List.of());
        service = new SettlementService(entryRepository, new EntryParts(entryPartRepository),
                personRepository, categoryRepository, settledMonthRepository, CLOCK);
        when(personRepository.findAll()).thenReturn(List.of(
                new PersonEntity("Alex"),
                new PersonEntity("Kim")));
    }

    @Test
    @DisplayName("Halbhalb: Alex zahlt 60 gemeinsam, Kim schuldet Alex 30")
    void halfAndHalf() {
        when(entryRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 1), "-60.00", "Essen", "Alex")));

        SettlementWithPositions settlement = service.settlement(NOVEMBER);

        assertThat(settlement.saldi())
                .containsEntry("Alex", new BigDecimal("30.00"))
                .containsEntry("Kim", new BigDecimal("-30.00"));
        assertThat(settlement.vorschlag()).isEqualTo("Kim schuldet Alex 30,00 €");
        assertThat(settlement.laeuftNoch()).isTrue();
        assertThat(settlement.positionen()).hasSize(1);
        assertThat(settlement.positionen().get(0).anteile())
                .containsEntry("Alex", new BigDecimal("30.00"))
                .containsEntry("Kim", new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Eigene Anteile 60/40: Alex zahlt 100 gemeinsam, Kim traegt 40")
    void customShares() {
        when(categoryRepository.findById("Essen")).thenReturn(Optional.of(
                new KategorieEntity("Essen", "ANTEILE", Map.of("Alex", 60, "Kim", 40))));
        when(entryRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 2), "-100.00", "Essen", "Alex")));

        SettlementWithPositions settlement = service.settlement(NOVEMBER);

        assertThat(settlement.saldi())
                .containsEntry("Alex", new BigDecimal("40.00"))
                .containsEntry("Kim", new BigDecimal("-40.00"));
        assertThat(settlement.positionen().get(0).anteile())
                .containsEntry("Alex", new BigDecimal("60.00"))
                .containsEntry("Kim", new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Gemeinsame Einnahme kehrt den Saldo um: Kim erhaelt 30, Alex bekommt 15")
    void sharedIncome() {
        when(entryRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 11, 3), "30.00", "Erstattung", "Kim")));

        SettlementWithPositions settlement = service.settlement(NOVEMBER);

        assertThat(settlement.saldi())
                .containsEntry("Alex", new BigDecimal("15.00"))
                .containsEntry("Kim", new BigDecimal("-15.00"));
    }

    @Test
    @DisplayName("Nur persoenliche Eintraege: kein Ausgleich, kein Vorschlag")
    void onlyPersonal() {
        when(entryRepository.findAll()).thenReturn(List.of(
                personal(LocalDate.of(2026, 11, 4), "-42.00", "Essen", "Alex")));

        SettlementWithPositions settlement = service.settlement(NOVEMBER);

        assertThat(settlement.saldi())
                .containsEntry("Alex", new BigDecimal("0.00"))
                .containsEntry("Kim", new BigDecimal("0.00"));
        assertThat(settlement.vorschlag()).isNull();
        assertThat(settlement.positionen()).isEmpty();
    }

    @Test
    @DisplayName("Eintraege anderer Monate zaehlen nicht")
    void otherMonth() {
        when(entryRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 10, 31), "-60.00", "Essen", "Alex")));

        SettlementWithPositions settlement = service.settlement(NOVEMBER);

        assertThat(settlement.positionen()).isEmpty();
        assertThat(settlement.vorschlag()).isNull();
    }

    @Test
    @DisplayName("Verlauf: ein Eintrag aus Oktober landet im Vormonat mit korrektem Saldo")
    void history() {
        when(entryRepository.findAll()).thenReturn(List.of(
                shared(LocalDate.of(2026, 10, 20), "-60.00", "Essen", "Alex")));

        List<SettlementMonth> history = service.history(2);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).monat()).isEqualTo("2026-10");
        assertThat(history.get(0).saldi()).containsEntry("Alex", new BigDecimal("30.00"));
        assertThat(history.get(1).monat()).isEqualTo("2026-11");
        assertThat(history.get(1).laeuftNoch()).isTrue();
    }

    private EntryEntity shared(LocalDate datum, String betrag, String kategorie, String zahler) {
        return new EntryEntity(datum, new BigDecimal(betrag), kategorie, null, true, zahler, null);
    }

    private EntryEntity personal(LocalDate datum, String betrag, String kategorie, String person) {
        return new EntryEntity(datum, new BigDecimal(betrag), kategorie, person, false, null, null);
    }
}
