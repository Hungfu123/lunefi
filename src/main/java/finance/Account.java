package finance;

import java.math.BigDecimal;

public record Account(Long id, String name, String person, BigDecimal startSaldo, BigDecimal saldo) {
}
