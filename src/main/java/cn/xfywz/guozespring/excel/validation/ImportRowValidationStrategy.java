package cn.xfywz.guozespring.excel.validation;

import java.util.function.Consumer;

@FunctionalInterface
public interface ImportRowValidationStrategy<T> {
    StepResult validate(T row, Consumer<String> addError);
}
