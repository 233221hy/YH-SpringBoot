package cn.xfywz.guozespring.excel.validation;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ValidationStrategies {
    private ValidationStrategies() {
    }

    public static <T> ImportRowValidationStrategy<T> uniqueInFile(
            Function<T, String> keyExtractor,
            Set<String> seenSet,
            Function<String, String> messageBuilder) {
        return (row, addError) -> {
            String key = keyExtractor.apply(row);
            if (key == null || key.isBlank()) {
                return StepResult.CONTINUE;
            }
            if (!seenSet.add(key)) {
                addError.accept(messageBuilder.apply(key));
                return StepResult.SKIP_GROUP;
            }
            return StepResult.CONTINUE;
        };
    }

    public static <T> ImportRowValidationStrategy<T> notExistsInSet(
            Function<T, String> keyExtractor,
            Set<String> existingSet,
            Function<String, String> messageBuilder) {
        return (row, addError) -> {
            String key = keyExtractor.apply(row);
            if (key == null || key.isBlank()) {
                return StepResult.CONTINUE;
            }
            if (existingSet.contains(key)) {
                addError.accept(messageBuilder.apply(key));
            }
            return StepResult.CONTINUE;
        };
    }

    public static <T, V> ImportRowValidationStrategy<T> keyExistsInMap(
            Function<T, String> keyExtractor,
            Map<String, V> referenceMap,
            Function<String, String> messageBuilder) {
        return (row, addError) -> {
            String key = keyExtractor.apply(row);
            if (key == null || key.isBlank()) {
                return StepResult.CONTINUE;
            }
            if (!referenceMap.containsKey(key)) {
                addError.accept(messageBuilder.apply(key));
            }
            return StepResult.CONTINUE;
        };
    }

    public static <T> ImportRowValidationStrategy<T> equalsExpectedValue(
            Function<T, String> keyExtractor,
            Function<T, String> actualValueExtractor,
            Map<String, String> expectedValueByKey,
            BiFunction<String, String, String> messageBuilder) {
        return (row, addError) -> {
            String key = keyExtractor.apply(row);
            if (key == null || key.isBlank()) {
                return StepResult.CONTINUE;
            }
            String expected = expectedValueByKey.get(key);
            if (expected == null) {
                return StepResult.CONTINUE;
            }
            String actual = actualValueExtractor.apply(row);
            if (actual == null) {
                return StepResult.CONTINUE;
            }
            if (!expected.trim().equals(actual.trim())) {
                addError.accept(messageBuilder.apply(key, expected));
            }
            return StepResult.CONTINUE;
        };
    }
}
