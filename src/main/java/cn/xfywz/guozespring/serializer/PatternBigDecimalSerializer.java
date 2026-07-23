package cn.xfywz.guozespring.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * BigDecimal 序列化器
 */
public class PatternBigDecimalSerializer extends JsonSerializer<BigDecimal> {
    private final DecimalFormat decimalFormat;
    public PatternBigDecimalSerializer(String pattern) {
        this.decimalFormat = new DecimalFormat(pattern);
        this.decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    }
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) { gen.writeNull(); return; }
        // 使用DecimalFormat格式化，确保输出指定位数的小数
        String formatted = decimalFormat.format(value);;
        gen.writeRawValue(formatted); // 输出数字
    }
}
