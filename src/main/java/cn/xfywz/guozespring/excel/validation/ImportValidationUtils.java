package cn.xfywz.guozespring.excel.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ImportValidationUtils {
    private ImportValidationUtils() {
    }

    public static <T> BiConsumer<T, Consumer<String>> toBusinessValidator(
            List<ValidationStep<T>> steps) {
        return (row, addError) -> {
            Set<String> skippedGroups = new HashSet<>();
            for (ValidationStep<T> step : steps) {
                if (step.getGroup() != null && skippedGroups.contains(step.getGroup())) {
                    continue;
                }
                StepResult result = step.execute(row, addError);
                if (result == StepResult.SKIP_GROUP) {
                    if (step.getGroup() != null) {
                        skippedGroups.add(step.getGroup());
                    }
                } else if (result == StepResult.STOP) {
                    return;
                }
            }
        };
    }
}
