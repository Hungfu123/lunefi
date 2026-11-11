package finance;

import java.util.Map;

public record SplitKonfiguration(String art, Map<String, Integer> anteile) {
}
