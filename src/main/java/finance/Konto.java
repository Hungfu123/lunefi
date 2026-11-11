package finance;

import java.math.BigDecimal;

public record Konto(Long id, String name, String person, BigDecimal startSaldo, BigDecimal saldo) {
}
