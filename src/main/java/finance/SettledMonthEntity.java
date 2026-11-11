package finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ausgleichsmonat")
public class SettledMonthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String monat;

    @Column(nullable = false)
    private boolean ausgeglichen;

    protected SettledMonthEntity() {
    }

    public SettledMonthEntity(String monat, boolean ausgeglichen) {
        this.monat = monat;
        this.ausgeglichen = ausgeglichen;
    }

    public Long getId() {
        return id;
    }

    public String getMonat() {
        return monat;
    }

    public boolean isAusgeglichen() {
        return ausgeglichen;
    }
}
