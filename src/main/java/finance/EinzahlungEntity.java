package finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "einzahlung")
public class EinzahlungEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal betrag;

    @Column(nullable = false)
    private String person;

    @Column(nullable = false)
    private LocalDate datum;

    protected EinzahlungEntity() {
    }

    public EinzahlungEntity(BigDecimal betrag, String person, LocalDate datum) {
        this.betrag = betrag;
        this.person = person;
        this.datum = datum;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBetrag() {
        return betrag;
    }

    public String getPerson() {
        return person;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public Einzahlung asDeposit() {
        return new Einzahlung(id, betrag, person, datum);
    }
}
