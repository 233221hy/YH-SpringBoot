package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 试题题库VO类（包含创建者姓名）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class YeeQuestionWithCreatorVO extends YeeQuestion {
    /**
     * 创建者姓名
     */
    private String creatorName;
}