package cn.xfywz.guozespring.excel;

import cn.xfywz.guozespring.excel.listener.ExcelReadListener;
import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Excel 导入链式构建器 — 编排导入流水线
 */
@Slf4j
public final class ExcelImportBuilder<T> {

    private final Class<T> entityClass;
    private InputStream inputStream;
    private Consumer<T> rowPreProcessor;
    private BiConsumer<T, Consumer<String>> businessValidator;
    private Function<List<T>, Integer> batchPersist;

    private ExcelImportBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public static <T> ExcelImportBuilder<T> of(Class<T> entityClass) {
        return new ExcelImportBuilder<>(entityClass);
    }

    public ExcelImportBuilder<T> from(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public ExcelImportBuilder<T> preprocess(Consumer<T> rowPreProcessor) {
        this.rowPreProcessor = rowPreProcessor;
        return this;
    }

    public ExcelImportBuilder<T> businessValidator(BiConsumer<T, Consumer<String>> validator) {
        this.businessValidator = validator;
        return this;
    }

    public ExcelImportBuilder<T> batchPersist(Function<List<T>, Integer> persister) {
        this.batchPersist = persister;
        return this;
    }

    public ImportResult execute() {
        long startTime = System.currentTimeMillis();
        try {
            ExcelReadListener<T> listener = new ExcelReadListener<>(entityClass, batchPersist);
            if (rowPreProcessor != null) {
                listener.setRowPreProcessor(rowPreProcessor);
            }
            if (businessValidator != null) {
                listener.setBusinessValidator(businessValidator);
            }

            EasyExcel.read(inputStream, entityClass, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();

            long duration = System.currentTimeMillis() - startTime;
            return ImportResult.fromListenerWithDb(listener, listener.getSuccessCount(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Excel导入失败", e);
            return ImportResult.systemError(e.getMessage(), duration);
        }
    }
}
