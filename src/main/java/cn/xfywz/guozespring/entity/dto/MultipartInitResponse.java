package cn.xfywz.guozespring.entity.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MultipartInitResponse {
    private boolean isNewUpload;      // 是否为新上传（true：新上传，false：断点续传）
    private boolean isInstantUpload;  // 是否为秒传（true：直接返回已有 URL）
    private String uploadId;
    private String objectKey;
    private String url;               // 秒传时的文件 URL
    private Map<String, Object> credentials; // 临时凭证（新上传或断点续传时返回）
    private String bucket;
    private String region;
}
