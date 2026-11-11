package finance;

public record Haltung(String haltung) {

    public static final String SPAREN = "SPAREN";
    public static final String FLEXIBEL = "FLEXIBEL";
    public static final String BEWUSST_MEHR = "BEWUSST_MEHR";

    public boolean istGueltig() {
        return SPAREN.equals(haltung) || FLEXIBEL.equals(haltung) || BEWUSST_MEHR.equals(haltung);
    }
}
