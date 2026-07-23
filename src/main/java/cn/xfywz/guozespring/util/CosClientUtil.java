package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.config.COSConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import org.joda.time.DateTime;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.io.ByteArrayInputStream; // 新增

public class CosClientUtil {
    public static String upload(MultipartFile file) {
        String secretId = COSConfig.ACCESS_KEY_ID;
        String secretKey = COSConfig.ACCESS_KEY_SECRET;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        Region region = new Region(COSConfig.END_POINT);
        ClientConfig clientConfig = new ClientConfig(region);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, clientConfig);

        // 存储桶的命名格式为 BucketName-APPID，此处填写的存储桶名称必须为此格式
        String bucketName = COSConfig.BUCKET_NAME;
        // 对象键(Key)是对象在存储桶中的唯一标识。  998u-09iu-09i-333
        //在文件名称前面添加uuid值
        String key = UUID.randomUUID().toString().replaceAll("-","")
                + Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));
        //对上传文件分组，根据当前日期  /2022/11/11
        String dateTime = new DateTime().toString("yyyy/MM/dd");
        key = dateTime+"/"+key;
        try {
            //获取上传文件输入流
            InputStream inputStream = file.getInputStream();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    key,
                    inputStream,
                    objectMetadata);
            // 高级接口会返回一个异步结果Upload
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            //返回上传文件路径
            //https://ggkt-atguigu-1310644373.cos.ap-beijing.myqcloud.com/01.jpg
            return "https://"+bucketName+"."+"cos"+"."+COSConfig.END_POINT+".myqcloud.com"+"/"+key;
        } catch (Exception e) {
            return null; // 失败返回 null，便于上层判断
        } finally {
            try { cosClient.shutdown(); } catch (Exception ignore) {}
        }
    }

    // 字节数组上传，用于CSV导出等场景
    public static String uploadBytes(byte[] data, String filename) {
        String secretId = COSConfig.ACCESS_KEY_ID;
        String secretKey = COSConfig.ACCESS_KEY_SECRET;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(COSConfig.END_POINT);
        ClientConfig clientConfig = new ClientConfig(region);
        COSClient cosClient = new COSClient(cred, clientConfig);

        String bucketName = COSConfig.BUCKET_NAME;
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf("."));
        }
        String key = UUID.randomUUID().toString().replaceAll("-","") + (ext.isEmpty() ? "" : ext);
        String dateTime = new DateTime().toString("yyyy/MM/dd");
        key = dateTime+"/"+key;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(data.length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    key,
                    inputStream,
                    objectMetadata);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            return "https://"+bucketName+"."+"cos"+"."+COSConfig.END_POINT+".myqcloud.com"+"/"+key;
        } catch (Exception e) {
            return null; // 失败返回 null
        } finally {
            try { cosClient.shutdown(); } catch (Exception ignore) {}
        }
    }
}

