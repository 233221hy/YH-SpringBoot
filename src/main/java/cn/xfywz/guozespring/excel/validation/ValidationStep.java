package cn.xfywz.guozespring.excel.validation;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ValidationStep<T> {

    private final ImportRowValidationStrategy<T> strategy;

    @Getter
    private final String group;

    private final Predicate<T> precondition;

    public ValidationStep(ImportRowValidationStrategy<T> strategy, String group) {
        this(strategy, group, null);
    }

    public ValidationStep(
            ImportRowValidationStrategy<T> strategy,
            String group,
            Predicate<T> precondition) {
        this.strategy = strategy;
        this.group = group;
        this.precondition = precondition;
    }

    public StepResult execute(T row, Consumer<String> addError) {
        if (precondition != null && !precondition.test(row)) {
            return StepResult.CONTINUE;
        }
        return strategy.validate(row, addError);
    }

}
