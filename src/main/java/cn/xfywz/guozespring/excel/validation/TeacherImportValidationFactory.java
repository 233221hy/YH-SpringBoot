package cn.xfywz.guozespring.excel.validation;

import cn.xfywz.guozespring.entity.dto.TeacherExcelData;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TeacherImportValidationFactory {
    private TeacherImportValidationFactory() {
    }

    public static TeacherImportValidationContext createContext(Set<String> existingAccounts) {
        return new TeacherImportValidationContext(existingAccounts);
    }

    public static BiConsumer<TeacherExcelData, Consumer<String>> createBusinessValidator(
            TeacherImportValidationContext ctx) {
        List<ValidationStep<TeacherExcelData>> steps = List.of(
                new ValidationStep<>(
                        ValidationStrategies.uniqueInFile(
                                TeacherExcelData::getLoginAccount,
                                ctx.getSeenAccounts(),
                                account -> "导入文件内账号重复: " + account
                        ),
                        "account"
                ),
                new ValidationStep<>(
                        ValidationStrategies.notExistsInSet(
                                TeacherExcelData::getLoginAccount,
                                ctx.getExistingAccounts(),
                                account -> "账号已存在: " + account
                        ),
                        "account"
                )
        );
        return ImportValidationUtils.toBusinessValidator(steps);
    }

    @Getter
    public static class TeacherImportValidationContext {
        private final Set<String> seenAccounts = new HashSet<>();
        private final Set<String> existingAccounts;

        private TeacherImportValidationContext(Set<String> existingAccounts) {
            this.existingAccounts = existingAccounts;
        }

    }
}
