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
public class SavingsGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "zielbetrag", nullable = false, precision = 19, scale = 2)
    private BigDecimal zielbetrag;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sparziel_id")
    private List<DepositEntity> einzahlungen = new ArrayList<>();

    protected SavingsGoalEntity() {
    }

    public SavingsGoalEntity(String name, BigDecimal zielbetrag) {
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

    public List<DepositEntity> getEinzahlungen() {
        return einzahlungen;
    }

    public SavingsGoal asSavingsGoal() {
        BigDecimal eingezahlt = einzahlungen.stream()
                .map(DepositEntity::getBetrag)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        boolean erreicht = eingezahlt.compareTo(zielbetrag) >= 0;
        return new SavingsGoal(id, name, zielbetrag, eingezahlt, erreicht,
                einzahlungen.stream().map(DepositEntity::asDeposit).toList());
    }
}
