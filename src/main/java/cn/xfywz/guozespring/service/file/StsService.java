package cn.xfywz.guozespring.service.file;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sts.v20180813.StsClient;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StsService {

    @Value("${cos.secretId}")
    private String secretId;
    @Value("${cos.secretKey}")
    private String secretKey;
    @Value("${cos.region}")
    private String region;
    @Value("${cos.bucketName}")
    private String bucketName;
    @Value("${sts.durationSeconds:1800}")
    private Integer defaultDurationSeconds;

    /**
     * 获取临时密钥
     * @param objectKey 允许操作的 COS 对象键（精确路径）
     * @param durationSeconds 有效期（秒），默认使用配置值
     * @return 临时凭证信息
     */
    public Map<String, Object> getTempCredential(String objectKey, Integer durationSeconds) {
        if (durationSeconds == null) {
            durationSeconds = defaultDurationSeconds;
        }
        try {
            // 构建权限策略（允许对指定 objectKey 进行分片上传相关操作）
            String policy = buildPolicy(objectKey);

            // 创建 STS 客户端
            Credential cred = new Credential(secretId, secretKey);
            StsClient client = new StsClient(cred, region);

            // 构造请求
            GetFederationTokenRequest req = new GetFederationTokenRequest();
            req.setName("temp-uploader-" + System.currentTimeMillis());
            req.setPolicy(policy);
            req.setDurationSeconds((long) durationSeconds);

            GetFederationTokenResponse resp = client.GetFederationToken(req);

            Map<String, Object> result = new HashMap<>();
            result.put("tmpSecretId", resp.getCredentials().getTmpSecretId());
            result.put("tmpSecretKey", resp.getCredentials().getTmpSecretKey());
            result.put("sessionToken", resp.getCredentials().getToken());
            result.put("expiration", resp.getExpiration());
            return result;
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("生成临时密钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建权限策略 JSON 字符串
     * 限制仅允许对指定 objectKey 进行分片上传相关操作
     */
    private String buildPolicy(String objectKey) {
        // 资源路径格式：qcs::cos:region:uid/appid:bucket/objectKey
        // 注意：bucketName 可能包含 appid，需要拼接正确
        // 这里简化，使用标准格式：qcs::cos:${region}:uid/${appid}:${bucket}/${objectKey}
        // 实际使用时，请根据您的 bucketName 格式调整（例如 bucketName 已包含 appid，则无需额外 uid）
        // 为了通用，假设 bucketName 格式为 "bucket-1234567890"，则 appid 为 1234567890
        String appid = bucketName.substring(bucketName.lastIndexOf("-") + 1);
        String resource = String.format("qcs::cos:%s:uid/%s:%s/%s",
                region, appid, bucketName, objectKey);

        // 允许的操作列表
        String[] actions = {
                "name/cos:InitiateMultipartUpload",
                "name/cos:UploadPart",
                "name/cos:CompleteMultipartUpload",
                "name/cos:AbortMultipartUpload",
                "name/cos:ListParts",
                "name/cos:PutObject",
                "name/cos:OptionsObject"
        };
        StringBuilder actionsStr = new StringBuilder();
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) actionsStr.append(",");
            actionsStr.append("\"").append(actions[i]).append("\"");
        }

        return String.format("{\n" +
                "  \"version\": \"2.0\",\n" +
                "  \"statement\": [\n" +
                "    {\n" +
                "      \"effect\": \"allow\",\n" +
                "      \"action\": [%s],\n" +
                "      \"resource\": [\"%s\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}", actionsStr, resource);
    }
}