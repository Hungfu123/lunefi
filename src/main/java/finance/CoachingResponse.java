package finance;

public record CoachingResponse(String monat, java.util.Map<String, java.math.BigDecimal> budgetsUebrig,
        java.util.List<CoachingRecommendation> empfehlungen, java.util.List<CoachingSaved> gutGespart) {
}
