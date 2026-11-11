package finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quelle")
public class QuelleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String art;

    @Column(nullable = false)
    private String konto;

    @Column(nullable = false)
    private String zustand = "offen";

    protected QuelleEntity() {
    }

    public QuelleEntity(String name, String art, String konto) {
        this.name = name;
        this.art = art;
        this.konto = konto;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArt() {
        return art;
    }

    public String getKonto() {
        return konto;
    }

    public String getZustand() {
        return zustand;
    }
}
