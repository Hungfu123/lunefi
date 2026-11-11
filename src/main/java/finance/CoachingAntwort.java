package finance;

public record CoachingAntwort(String monat, java.util.Map<String, java.math.BigDecimal> budgetsUebrig,
        java.util.List<CoachingEmpfehlung> empfehlungen, java.util.List<CoachingErspart> gutGespart) {
}
