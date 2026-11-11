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
@Table(name = "eintrag")
public class EintragEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal betrag;

    @Column(nullable = false)
    private String kategorie;

    private String person;

    @Column(nullable = false)
    private Boolean gemeinsam;

    private String zahler;

    private String konto;

    private Long quelleId;

    private String externeKennung;

    private String verwendungszweck;

    protected EintragEntity() {
    }

    public EintragEntity(LocalDate datum, BigDecimal betrag, String kategorie,
            String person, Boolean gemeinsam, String zahler, String konto) {
        this.datum = datum;
        this.betrag = betrag;
        this.kategorie = kategorie;
        this.person = person;
        this.gemeinsam = gemeinsam;
        this.zahler = zahler;
        this.konto = konto;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDatum() {
        return datum;
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

    public Boolean getGemeinsam() {
        return gemeinsam;
    }

    public String getZahler() {
        return zahler;
    }

    public String getKonto() {
        return konto;
    }

    public Long getQuelleId() {
        return quelleId;
    }

    public void setQuelleId(Long quelleId) {
        this.quelleId = quelleId;
    }

    public String getExterneKennung() {
        return externeKennung;
    }

    public void setExterneKennung(String externeKennung) {
        this.externeKennung = externeKennung;
    }

    public String getVerwendungszweck() {
        return verwendungszweck;
    }

    public void setVerwendungszweck(String verwendungszweck) {
        this.verwendungszweck = verwendungszweck;
    }

    public Eintrag alsEintrag() {
        return new Eintrag(id, datum, betrag, kategorie, person, gemeinsam, zahler, konto);
    }
}
