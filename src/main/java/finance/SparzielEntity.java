package finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sparziel")
public class SparzielEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "zielbetrag", nullable = false, precision = 19, scale = 2)
    private BigDecimal zielbetrag;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sparziel_id")
    private List<EinzahlungEntity> einzahlungen = new ArrayList<>();

    protected SparzielEntity() {
    }

    public SparzielEntity(String name, BigDecimal zielbetrag) {
        this.name = name;
        this.zielbetrag = zielbetrag;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getZielbetrag() {
        return zielbetrag;
    }

    public List<EinzahlungEntity> getEinzahlungen() {
        return einzahlungen;
    }

    public Sparziel alsSparziel() {
        BigDecimal eingezahlt = einzahlungen.stream()
                .map(EinzahlungEntity::getBetrag)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        boolean erreicht = eingezahlt.compareTo(zielbetrag) >= 0;
        return new Sparziel(id, name, zielbetrag, eingezahlt, erreicht,
                einzahlungen.stream().map(EinzahlungEntity::alsEinzahlung).toList());
    }
}
