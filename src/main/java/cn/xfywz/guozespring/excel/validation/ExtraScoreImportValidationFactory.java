package cn.xfywz.guozespring.excel.validation;

import cn.xfywz.guozespring.entity.dto.ExtraScoreExcelData;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ExtraScoreImportValidationFactory {
    private ExtraScoreImportValidationFactory() {
    }

    public static ExtraScoreImportValidationContext createContext(Map<String, String> stuNameByNumber) {
        return new ExtraScoreImportValidationContext(stuNameByNumber);
    }

    public static BiConsumer<ExtraScoreExcelData, Consumer<String>> createBusinessValidator(
            ExtraScoreImportValidationContext ctx) {
        List<ValidationStep<ExtraScoreExcelData>> steps = List.of(
                new ValidationStep<>(
                        ValidationStrategies.uniqueInFile(
                                ExtraScoreExcelData::getStudentNumber,
                                ctx.getSeenNumbers(),
                                num -> "导入文件内学号重复: " + num
                        ),
                        "number"
                ),
                new ValidationStep<>(
                        ValidationStrategies.keyExistsInMap(
                                ExtraScoreExcelData::getStudentNumber,
                                ctx.getStuNameByNumber(),
                                num -> "学号不存在或不属于该课程: " + num
                        ),
                        "number"
                ),
                new ValidationStep<>(
                        ValidationStrategies.equalsExpectedValue(
                                ExtraScoreExcelData::getStudentNumber,
                                ExtraScoreExcelData::getStudentName,
                                ctx.getStuNameByNumber(),
                                (num, expectedName) -> "姓名与学号不匹配: " + num + "（应为" + expectedName + "）"
                        ),
                        "number"
                )
        );
        return ImportValidationUtils.toBusinessValidator(steps);
    }

    @Getter
    public static class ExtraScoreImportValidationContext {
        private final Set<String> seenNumbers = new HashSet<>();
        private final Map<String, String> stuNameByNumber;

        private ExtraScoreImportValidationContext(Map<String, String> stuNameByNumber) {
            this.stuNameByNumber = stuNameByNumber != null ? stuNameByNumber : new HashMap<>();
        }

    }
}
