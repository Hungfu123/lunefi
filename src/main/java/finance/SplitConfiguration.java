package finance;

import java.util.Map;

public record SplitConfiguration(String art, Map<String, Integer> anteile) {
}
