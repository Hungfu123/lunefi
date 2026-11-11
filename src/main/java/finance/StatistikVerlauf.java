package finance;

import java.math.BigDecimal;

public record StatistikVerlauf(String kategorie, java.util.List<StatistikMonat> monate,
        BigDecimal median, BigDecimal durchschnitt) {
}
