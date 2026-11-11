package finance;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "konto")
public class KontoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String person;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal startSaldo;

    protected KontoEntity() {
    }

    public KontoEntity(String name, String person, BigDecimal startSaldo) {
        this.name = name;
        this.person = person;
        this.startSaldo = startSaldo;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPerson() {
        return person;
    }

    public BigDecimal getStartSaldo() {
        return startSaldo;
    }
}
