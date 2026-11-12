package finance;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eintrag_teil")
public class EntryPartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eintrag_id", nullable = false)
    private Long eintragId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal betrag;

    @Column(nullable = false)
    private String kategorie;

    private String person;

    @Column(nullable = false)
    private boolean gemeinsam;

    protected EntryPartEntity() {
    }

    public EntryPartEntity(Long eintragId, int position, BigDecimal betrag, String kategorie,
            String person, boolean gemeinsam) {
        this.eintragId = eintragId;
        this.position = position;
        this.betrag = betrag;
        this.kategorie = kategorie;
        this.person = person;
        this.gemeinsam = gemeinsam;
    }

    public Long getId() {
        return id;
    }

    public Long getEintragId() {
        return eintragId;
    }

    public int getPosition() {
        return position;
    }

    public BigDecimal getBetrag() {
        return betrag;
    }

    public String getKategorie() {
        return kategorie;
    }

    public String getPerson() {
        return person;
    }

    public boolean isGemeinsam() {
        return gemeinsam;
    }

    public EntryPart asPart() {
        return new EntryPart(betrag, kategorie, person, gemeinsam);
    }
}
