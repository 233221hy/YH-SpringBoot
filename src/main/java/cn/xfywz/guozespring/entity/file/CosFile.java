package cn.xfywz.guozespring.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("cos_file") // 指定表名
public class CosFile {

    // CosFile.java
    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("object_key")
    private String objectKey;

    @TableField("file_size")
    private Long fileSize;

    @TableField("upload_id")
    private String uploadId;

    @TableField("status")
    private String status;

    @TableField("url")
    private String url;

    @TableField("file_hash")
    private String fileHash;      // 文件整体 MD5

    @TableField("total_parts")
    private Integer totalParts;   // 总分片数（用于校验）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("uniq_hash")
    private String uniqHash;
}