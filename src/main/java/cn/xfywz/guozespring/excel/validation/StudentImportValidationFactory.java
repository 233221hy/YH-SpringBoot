package cn.xfywz.guozespring.excel.validation;

import cn.xfywz.guozespring.entity.dto.StudentExcelData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StudentImportValidationFactory {
    private StudentImportValidationFactory() {
    }

    public static StudentImportValidationContext createContext(
            Set<String> existingNumbers,
            Map<String, Long> collegeMap,
            Map<String, Long> classMap) {
        return new StudentImportValidationContext(existingNumbers, collegeMap, classMap);
    }

    public static BiConsumer<StudentExcelData, Consumer<String>> createBusinessValidator(
            StudentImportValidationContext ctx) {
        List<ValidationStep<StudentExcelData>> steps = List.of(
                new ValidationStep<>(
                        ValidationStrategies.uniqueInFile(
                                StudentExcelData::getNumber,
                                ctx.getSeenNumbers(),
                                number -> "导入文件内学号重复: " + number
                        ),
                        "number"
                ),
                new ValidationStep<>(
                        ValidationStrategies.notExistsInSet(
                                StudentExcelData::getNumber,
                                ctx.getExistingNumbers(),
                                number -> "学号已存在: " + number
                        ),
                        "number"
                ),
                new ValidationStep<>(
                        ValidationStrategies.keyExistsInMap(
                                StudentExcelData::getCollege,
                                ctx.getCollegeMap(),
                                college -> "学院不存在: " + college
                        ),
                        "college"
                ),
                new ValidationStep<>(
                        ValidationStrategies.keyExistsInMap(
                                StudentExcelData::getStuClass,
                                ctx.getClassMap(),
                                cls -> "班级不存在: " + cls
                        ),
                        "class"
                )
        );

        return ImportValidationUtils.toBusinessValidator(steps);
    }

    public static class StudentImportValidationContext {
        private final Set<String> seenNumbers = new HashSet<>();
        private final Set<String> existingNumbers;
        private final Map<String, Long> collegeMap;
        private final Map<String, Long> classMap;

        private StudentImportValidationContext(Set<String> existingNumbers, Map<String, Long> collegeMap, Map<String, Long> classMap) {
            this.existingNumbers = existingNumbers;
            this.collegeMap = collegeMap;
            this.classMap = classMap;
        }

        public Set<String> getSeenNumbers() {
            return seenNumbers;
        }

        public Set<String> getExistingNumbers() {
            return existingNumbers;
        }

        public Map<String, Long> getCollegeMap() {
            return collegeMap;
        }

        public Map<String, Long> getClassMap() {
            return classMap;
        }
    }
}
