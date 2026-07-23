// CosFilePart.java
package cn.xfywz.guozespring.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cos_file_part")
public class CosFilePart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;           // 关联 CosFile.id
    private Integer partNumber;    // 分片序号（从1开始）
    private String eTag;           // COS 返回的 ETag

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}