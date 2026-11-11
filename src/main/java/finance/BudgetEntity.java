package finance;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "budget")
public class BudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String monat;

    @Column(nullable = false)
    private String kategorie;

    @Column(name = "budget_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal limit;

    protected BudgetEntity() {
    }

    public BudgetEntity(String monat, String kategorie, BigDecimal limit) {
        this.monat = monat;
        this.kategorie = kategorie;
        this.limit = limit;
    }

    public Long getId() {
        return id;
    }

    public String getMonat() {
        return monat;
    }

    public String getKategorie() {
        return kategorie;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

    public Budget alsBudget() {
        return new Budget(monat, kategorie, limit);
    }
}
