package finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lernregel")
public class LearningRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kennung;

    @Column(nullable = false)
    private String kategorie;

    protected LearningRuleEntity() {
    }

    public LearningRuleEntity(String kennung, String kategorie) {
        this.kennung = kennung;
        this.kategorie = kategorie;
    }

    public Long getId() {
        return id;
    }

    public String getKennung() {
        return kennung;
    }

    public String getKategorie() {
        return kategorie;
    }
}
