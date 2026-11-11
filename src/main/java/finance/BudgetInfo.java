package finance;

import java.math.BigDecimal;

public record BudgetInfo(BigDecimal limit, BigDecimal ist, boolean warnung) {
}
