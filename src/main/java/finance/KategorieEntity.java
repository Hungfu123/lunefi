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

    protected KategorieEntity() {
    }

    public KategorieEntity(String name, String art, Map<String, Integer> anteile) {
        this.name = name;
        this.art = art;
        this.anteile = anteile;
    }

    public String getName() {
        return name;
    }

    public String getArt() {
        return art;
    }

    public Map<String, Integer> getAnteile() {
        return anteile;
    }

    public SplitKonfiguration asSplitConfiguration() {
        return new SplitKonfiguration(art, anteile.isEmpty() ? null : Map.copyOf(anteile));
    }
}
