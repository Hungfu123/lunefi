package finance;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "kategorie")
public class KategorieEntity {

    @Id
    private String name;

    private String art;

    @ElementCollection
    @CollectionTable(name = "kategorie_anteil")
    @MapKeyColumn(name = "person")
    @Column(name = "anteil")
    private Map<String, Integer> anteile = new HashMap<>();

    private String haltung;

    protected KategorieEntity() {
    }

    public KategorieEntity(String name, String art, Map<String, Integer> anteile) {
        this(name, art, anteile, null);
    }

    public KategorieEntity(String name, String art, Map<String, Integer> anteile, String haltung) {
        this.name = name;
        this.art = art;
        this.anteile = anteile;
        this.haltung = haltung;
    }

    public String getName() {
        return name;
    }

    public String getArt() {
        return art;
    }

    public void setArt(String art) {
        this.art = art;
    }

    public Map<String, Integer> getAnteile() {
        return anteile;
    }

    public String getHaltung() {
        return haltung;
    }

    public void setAttitude(String haltung) {
        this.haltung = haltung;
    }

    public String asAttitude() {
        return haltung == null ? "FLEXIBEL" : haltung;
    }

    public SplitConfiguration asSplitConfiguration() {
        return new SplitConfiguration(art, anteile.isEmpty() ? null : Map.copyOf(anteile));
    }
}
