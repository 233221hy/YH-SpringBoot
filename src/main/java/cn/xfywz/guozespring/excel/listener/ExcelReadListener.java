package cn.xfywz.guozespring.excel.listener;

import cn.xfywz.guozespring.excel.RawImportError;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import jakarta.validation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Excel读取监听器 - 只负责读取和收集原始错误
 */
@Slf4j
@Getter
public class ExcelReadListener<T> implements ReadListener<T> {

    // 批处理大小
    private static final int BATCH_COUNT = 1000;

    // 缓存解析到的数据列表
    private final List<T> cachedDataList;

    // 数据处理函数
    private final Function<List<T>, Integer> dataConsumer;

    // 校验器
    private final Validator validator;

    // 原始错误信息列表
    private final List<RawImportError> rawErrors;

    // 存储校验失败的数据列表
    private final List<T> failedDataList;

    // 总解析数据条数
    private int totalCount = 0;

    // 成功处理的数据条数
    private int successCount = 0;

    /**
     * -- SETTER --
     *  设置业务校验器
     */
    // 业务校验器
    @Setter
    private BiConsumer<T, Consumer<String>> businessValidator;

    /**
     * -- SETTER --
     *  设置数据预处理器
     */
    // 数据预处理器
    @Setter
    private Consumer<T> rowPreProcessor;

    // 实体类的Class对象
    private final Class<T> clazz;

    /**
     * 构造函数
     */
    public ExcelReadListener(Class<T> clazz, Function<List<T>, Integer> dataConsumer) {
        this.clazz = clazz;
        this.dataConsumer = dataConsumer;
        this.cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        this.failedDataList = new ArrayList<>();
        this.rawErrors = new ArrayList<>();

        // 初始化校验器工厂
        try (ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()) {
            this.validator = factory.getValidator();
        }

        log.debug("创建ExcelReadListener for {}", clazz.getSimpleName());
    }

    /**
     * 每一条数据解析都会来调用
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        totalCount++;
        int currentRow = context.readRowHolder().getRowIndex() + 1; // Excel行号从1开始

        // 数据预处理
        if (rowPreProcessor != null) {
            try {
                rowPreProcessor.accept(data);
            } catch (Exception e) {
                rawErrors.add(RawImportError.dataConvertError(currentRow, "数据预处理", e.getMessage()));
                failedDataList.add(data);
                log.warn("第{}行数据预处理失败: {}", currentRow, e.getMessage());
                return;
            }
        }

        // JSR-303验证
        Set<ConstraintViolation<T>> violations = validator.validate(data);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<T> violation : violations) {
                String fieldName = violation.getPropertyPath().toString();
                rawErrors.add(RawImportError.validationError(currentRow, fieldName, violation.getMessage()));
            }
            failedDataList.add(data);
            return;
        }

        // 业务校验
        if (businessValidator != null) {
            List<String> businessErrorMessages = new ArrayList<>();
            businessValidator.accept(data, businessErrorMessages::add);

            if (!businessErrorMessages.isEmpty()) {
                for (String errorMsg : businessErrorMessages) {
                    rawErrors.add(RawImportError.businessError(currentRow, errorMsg));
                }
                failedDataList.add(data);
                return;
            }
        }

        // 校验通过，添加到缓存列表
        cachedDataList.add(data);

        // 处理批量数据
        if (cachedDataList.size() >= BATCH_COUNT) {
            processBatch();
            cachedDataList.clear();
        }
    }

    /**
     * 所有数据解析完成后会调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!cachedDataList.isEmpty()) {
            processBatch();
        }

    }

    /**
     * 异常处理
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        if (exception instanceof ExcelDataConvertException excelException) {
            int columnIndex = excelException.getColumnIndex();
            int rowIndex = excelException.getRowIndex() + 1;

            rawErrors.add(RawImportError.dataConvertError(
                    rowIndex,
                    String.valueOf(columnIndex),
                    "数据转换失败: " + excelException.getMessage()
            ));
        } else {
            log.error("解析Excel异常", exception);
            rawErrors.add(RawImportError.dataConvertError(
                    totalCount + 1,
                    "SYSTEM",
                    "解析异常: " + exception.getMessage()
            ));
        }
    }

    /**
     * 处理批量数据
     */
    private void processBatch() {
        if (dataConsumer != null && !cachedDataList.isEmpty()) {
            try {
                int processedSuccess = dataConsumer.apply(cachedDataList);
                if (processedSuccess > 0) {
                    successCount += processedSuccess;
                    log.debug("批量处理成功 {} 条数据", processedSuccess);
                }
            } catch (Exception e) {
                log.error("处理数据异常", e);
                rawErrors.add(RawImportError.dataConvertError(
                        0,
                        "BATCH_PROCESS",
                        "批量处理异常: " + e.getMessage()
                ));
            }
        }
    }

    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !rawErrors.isEmpty();
    }

    /**
     * 获取有效数据
     */
    public List<T> getValidData() {
        return Collections.unmodifiableList(cachedDataList);
    }

    /**
     * 获取所有数据（包括失败的）
     */
    public List<T> getAllData() {
        List<T> allData = new ArrayList<>(cachedDataList);
        allData.addAll(failedDataList);
        return Collections.unmodifiableList(allData);
    }
}