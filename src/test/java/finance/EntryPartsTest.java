package finance;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntryPartsTest {

    private static final BigDecimal THIRTY = new BigDecimal("-30.00");

    private EntryParts entryParts;
    private EntryPartRepository partRepository;

    @BeforeEach
    void setUp() {
        partRepository = mock(EntryPartRepository.class);
        when(partRepository.findAllByOrderByEintragIdAscPositionAsc()).thenReturn(List.of());
        entryParts = new EntryParts(partRepository);
    }

    @Test
    @DisplayName("Teile sind gültig, wenn sie cent-genau aufgehen")
    void validPartsSumUpExactly() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, true),
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isTrue();
    }

    @Test
    @DisplayName("Teile mit falscher Summe sind ungültig")
    void invalidSum() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, true),
                new EntryPart(new BigDecimal("-9.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isFalse();
    }

    @Test
    @DisplayName("Ein Teil mit falschem Vorzeichen ist ungültig")
    void invalidSign() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, true),
                new EntryPart(new BigDecimal("10.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isFalse();
    }

    @Test
    @DisplayName("Ein Teil mit Person UND gemeinsam ist ungültig")
    void invalidDoubleAssignment() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", "Alex", true),
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isFalse();
    }

    @Test
    @DisplayName("Ein Teil ohne Person und ohne gemeinsam ist ungültig")
    void invalidNoAssignment() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, false),
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isFalse();
    }

    @Test
    @DisplayName("Ein Teil ohne Kategorie ist ungültig")
    void invalidWithoutCategory() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.00"), " ", null, true),
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, THIRTY)).isFalse();
    }

    @Test
    @DisplayName("Der primäre Teil ist der betragsgrößte")
    void primaryIsLargestPart() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false),
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, true));
        EntryPart primary = entryParts.primary(parts);
        assertThat(primary.kategorie()).isEqualTo("Essen");
        assertThat(primary.gemeinsam()).isTrue();
        assertThat(primary.person()).isNull();
    }

    @Test
    @DisplayName("Bei Gleichstand gewinnt der erste Teil")
    void primaryTieKeepsFirst() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-15.00"), "Essen", null, true),
                new EntryPart(new BigDecimal("-15.00"), "Geschenk", "Alex", false));
        assertThat(entryParts.primary(parts).kategorie()).isEqualTo("Essen");
    }

    @Test
    @DisplayName("anyShared erkennt gemeinsame Teile")
    void anySharedDetectsSharedPart() {
        List<EntryPart> mixed = List.of(
                new EntryPart(new BigDecimal("-20.00"), "Essen", null, true),
                new EntryPart(new BigDecimal("-10.00"), "Geschenk", "Alex", false));
        List<EntryPart> allPrivate = List.of(
                new EntryPart(new BigDecimal("-30.00"), "Essen", "Alex", false));
        assertThat(entryParts.anyShared(mixed)).isTrue();
        assertThat(entryParts.anyShared(allPrivate)).isFalse();
        assertThat(entryParts.anyShared(null)).isFalse();
    }

    @Test
    @DisplayName("Teile mit mehr als zwei Nachkommastellen sind ungültig")
    void invalidSubCentScale() {
        List<EntryPart> parts = List.of(
                new EntryPart(new BigDecimal("-20.005"), "Essen", null, true),
                new EntryPart(new BigDecimal("-10.005"), "Geschenk", "Alex", false));
        assertThat(entryParts.isValid(parts, new BigDecimal("-30.01"))).isFalse();
    }
}
