package cn.xfywz.guozespring.service.file;

import cn.xfywz.guozespring.entity.dto.MultipartInitResponse;
import cn.xfywz.guozespring.entity.file.CosFile;
import cn.xfywz.guozespring.entity.file.CosFilePart;
import cn.xfywz.guozespring.mapper.file.CosFileMapper;
import cn.xfywz.guozespring.mapper.file.CosFilePartMapper;
import cn.xfywz.guozespring.util.FileResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.xfywz.guozespring.entity.file.CosFile.*;

@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<CosFileMapper, CosFile> {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Value("${upload.chunkSize}")
    private long CHUNK_SIZE;
    @Value("${cos.bucketName}")
    private String bucketName;
    @Value("${cos.region}")
    private String region;

    private final COSClient cosClient;
    private final CosFilePartMapper cosFilePartMapper;
    private final StsService stsService;

    /**
     * 初始化分片上传（使用 UUID 生成唯一 objectKey）
     */
//    public FileResult<String> initMultipartUpload(String fileName, Long fileSize, String fileHash) {
//        // 可选：检查是否已有相同 hash 的已完成文件（实现秒传）
//        // 此处为简化，不实现完整秒传，仅防重复初始化
//
//        LambdaQueryWrapper<CosFile> existingWrapper = new LambdaQueryWrapper<>();
//        existingWrapper.eq(CosFile::getFileHash, fileHash)
//                .eq(CosFile::getStatus, CosFile.STATUS_INIT);
//        CosFile existing = this.getOne(existingWrapper);
//        if (existing != null) {
//            return FileResult.success(existing.getUploadId());
//        }
//
//        // ✅ 生成唯一 objectKey
//        String fileExt = "";
//        if (fileName != null && fileName.lastIndexOf(".") > 0) {
//            fileExt = fileName.substring(fileName.lastIndexOf("."));
//        }
//        String objectKey = "Multipart/" + UUID.randomUUID().toString() + fileExt;
//
//        // 发起 COS 初始化
//        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
//        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);
//        String uploadId = result.getUploadId();
//
//        long totalParts = (fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE;
//
//        // 保存记录：原始名 + 唯一 objectKey
//        CosFile file = new CosFile();
//        file.setFileName(fileName);          // 原始文件名（用于展示/下载）
//        file.setFileSize(fileSize);
//        file.setFileHash(fileHash);
//        file.setTotalParts((int) totalParts);
//        file.setUploadId(uploadId);
//        file.setObjectKey(objectKey);        // ✅ 唯一存储路径
//        file.setStatus(CosFile.STATUS_INIT);
//        this.save(file);
//
//        return FileResult.success(uploadId);
//    }
    public FileResult<MultipartInitResponse> initMultipartUpload(String fileName, Long fileSize, String fileHash) {

        // 参数校验
        if (fileName == null || fileName.trim().isEmpty()) {
            return FileResult.error("文件名不能为空");
        }
        if (fileSize == null || fileSize <= 0) {
            return FileResult.error("文件大小无效");
        }
        if (fileHash == null || fileHash.isEmpty()) {
            return FileResult.error("文件哈希不能为空");
        }
        // 1. 秒传检查：是否存在相同 hash 且状态为已完成
        LambdaQueryWrapper<CosFile> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(CosFile::getFileHash, fileHash)
                .eq(CosFile::getStatus, CosFile.STATUS_COMPLETED);
//        CosFile completedFile = this.getOne(completedWrapper);
        List<CosFile> completedFiles = this.list(completedWrapper);
        if (completedFiles.size() > 1) {
            log.warn("发现多个已完成文件记录, fileHash={}, 数量={}", fileHash, completedFiles.size());
        }
        CosFile completedFile = null;
        if (!completedFiles.isEmpty()) {
            // 如果有多条，按创建时间倒序取最新的一条
            completedFile = completedFiles.stream()
                    .max(Comparator.comparing(CosFile::getCreateTime))
                    .orElse(null);
        }
        if (completedFile != null) {
            MultipartInitResponse resp = new MultipartInitResponse();
            resp.setInstantUpload(true);
            resp.setUrl(completedFile.getUrl());
            return FileResult.success(resp);
        }

        // 2. 断点续传检查：是否存在相同 hash 且状态为初始化或上传中
        LambdaQueryWrapper<CosFile> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(CosFile::getFileHash, fileHash)
                .in(CosFile::getStatus, Arrays.asList(STATUS_INIT, STATUS_UPLOADING));
//        CosFile existingFile = this.getOne(existingWrapper);
        List<CosFile> existingFiles = this.list(existingWrapper);
        CosFile existingFile = null;
        if (!existingFiles.isEmpty()) {
            existingFile = existingFiles.stream()
                    .max(Comparator.comparing(CosFile::getCreateTime))
                    .orElse(null);
        }
        if (existingFile != null) {
            // 已有未完成的上传记录，返回该 uploadId，并生成新的临时密钥
            String uploadId = existingFile.getUploadId();
            String objectKey = existingFile.getObjectKey();

            // 生成临时密钥（针对该 objectKey）
            Map<String, Object> credentials = stsService.getTempCredential(objectKey, null);

            MultipartInitResponse resp = new MultipartInitResponse();
            resp.setNewUpload(false);
            resp.setUploadId(uploadId);
            resp.setObjectKey(objectKey);
            resp.setCredentials(credentials);
            resp.setBucket(bucketName);
            resp.setRegion(region);
            return FileResult.success(resp);
        }

        // 3. 全新上传：生成 objectKey，调用 COS 初始化
        String fileExt = "";
        if (fileName != null && fileName.lastIndexOf(".") > 0) {
            fileExt = fileName.substring(fileName.lastIndexOf("."));
            // 限制扩展名长度，防止异常
            if (fileExt.length() > 10) {
                fileExt = "";
            }
        }
//        String objectKey = "Multipart/" + UUID.randomUUID() + fileExt;
        // 生成 objectKey 时，只使用安全字符
        String objectKey = "Multipart/" + UUID.randomUUID().toString() +
                (fileExt.isEmpty() ? "" : fileExt.toLowerCase());

        // 发起 COS 初始化
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);
        String uploadId = result.getUploadId();

        if (uploadId == null || uploadId.isEmpty()) {
            return FileResult.error("COS 初始化失败");
        }


        long totalParts = (fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE;

        // 保存文件记录
        CosFile file = new CosFile();
        file.setFileName(fileName);
        file.setFileSize(fileSize);
        file.setFileHash(fileHash);
        file.setTotalParts((int) totalParts);
        file.setUploadId(uploadId);
        file.setObjectKey(objectKey);
        file.setStatus(STATUS_INIT);
        //设置 uniq_hash
        file.setUniqHash(fileHash);
//        this.save(file);
        try {
            this.save(file);
        } catch (DuplicateKeyException e) {
            // 唯一约束冲突，说明另一个请求已经创建了相同 fileHash 的记录
            // 重新查询未完成的记录
            LambdaQueryWrapper<CosFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CosFile::getFileHash, fileHash)
                    .in(CosFile::getStatus, Arrays.asList(STATUS_INIT, STATUS_UPLOADING));
            CosFile existing = this.getOne(wrapper);
            if (existing != null) {
                // 返回已有记录的信息（断点续传）
                Map<String, Object> credentials = stsService.getTempCredential(existing.getObjectKey(), null);
                MultipartInitResponse resp = new MultipartInitResponse();
                resp.setNewUpload(false);
                resp.setUploadId(existing.getUploadId());
                resp.setObjectKey(existing.getObjectKey());
                resp.setCredentials(credentials);
                resp.setBucket(bucketName);
                resp.setRegion(region);
                return FileResult.success(resp);
            } else {
                // 理论上不应该发生，但若发生则返回错误
                return FileResult.error("初始化冲突，请稍后重试");
            }
        }


        // 生成临时密钥 （只在 save 成功时执行）
        Map<String, Object> credentials = stsService.getTempCredential(objectKey, null);

        MultipartInitResponse resp = new MultipartInitResponse();
        resp.setNewUpload(true);
        resp.setUploadId(uploadId);
        resp.setObjectKey(objectKey);
        resp.setCredentials(credentials);
        resp.setBucket(bucketName);
        resp.setRegion(region);
        return FileResult.success(resp);
    }

    /**
     * 上传分片（使用数据库中的 objectKey，而非前端传的 fileName）
     */
//    public FileResult<Void> uploadPart(String uploadId, Integer partNumber, MultipartFile file) {
//        try (InputStream is = file.getInputStream()) {
//            CosFile fileRecord = this.getOne(new LambdaQueryWrapper<CosFile>().eq(CosFile::getUploadId, uploadId));
//            if (fileRecord == null) {
//                return FileResult.error("上传记录不存在");
//            }
//            if (partNumber < 1 || partNumber > fileRecord.getTotalParts()) {
//                return FileResult.error("分片序号超出范围");
//            }
//
//            // 使用 objectKey 而非 fileName
//            UploadPartRequest uploadRequest = new UploadPartRequest();
//            uploadRequest.setBucketName(bucketName);
//            uploadRequest.setKey(fileRecord.getObjectKey());
//            uploadRequest.setUploadId(uploadId);
//            uploadRequest.setPartNumber(partNumber);
//            uploadRequest.setInputStream(is);
//            uploadRequest.setPartSize(file.getSize());
//
//            // 执行上传
//            UploadPartResult uploadPartResult = cosClient.uploadPart(uploadRequest);
//            PartETag partETag = uploadPartResult.getPartETag();
//
//            // 保存分片元数据
//            CosFilePart part = new CosFilePart();
//            part.setFileId(fileRecord.getId());
//            part.setPartNumber(partNumber);
//            part.setETag(partETag.getETag());
//
//            try {
//                cosFilePartMapper.insertIgnore(part);
//            } catch (Exception e) {
//                if (!isDuplicateKeyException(e)) {
//                    return FileResult.error("保存分片元数据失败");
//                }
//                // 如果是重复键异常，说明已经保存过，直接返回成功
//            }
//
//            return FileResult.success();
//        } catch (IOException e) {
//            return FileResult.error("IO 异常：" + e.getMessage());
//        }
//    }

    /**
     * 前端上传分片后，上报分片信息
     */
    public FileResult<Void> reportPart(String uploadId, Integer partNumber, String eTag) {
        CosFile file = this.getOne(new LambdaQueryWrapper<CosFile>().eq(CosFile::getUploadId, uploadId));
        if (file == null) {
            return FileResult.error("上传记录不存在");
        }
        if (partNumber < 1 || partNumber > file.getTotalParts()) {
            return FileResult.error("分片序号超出范围");
        }

        // 验证 ETag 格式（COS 的 ETag 通常是 32 位十六进制字符串，可能带引号）
        if (eTag == null || eTag.trim().isEmpty()) {
            return FileResult.error("ETag 不能为空");
        }

        // 去除 ETag 两端的引号（如果有的话）
        String cleanETag = eTag.trim();
        if (cleanETag.startsWith("\"") && cleanETag.endsWith("\"")) {
            cleanETag = cleanETag.substring(1, cleanETag.length() - 1);
        }

        // 如果状态是 INIT，更新为 UPLOADING（表示已开始上传，uniqHash 不变）
        if (STATUS_INIT.equals(file.getStatus())) {
            file.setStatus(STATUS_UPLOADING);
//            this.updateById(file);
            // 使用 update 方法，避免再次查询
            this.update(new LambdaUpdateWrapper<CosFile>()
                    .eq(CosFile::getId, file.getId())
                    .eq(CosFile::getStatus, CosFile.STATUS_INIT)  // 乐观锁，防止并发覆盖
                    .set(CosFile::getStatus, CosFile.STATUS_UPLOADING));
        }

        // 保存分片记录（去重）
        CosFilePart part = new CosFilePart();
        part.setFileId(file.getId());
        part.setPartNumber(partNumber);
        part.setETag(eTag);
        try {
            cosFilePartMapper.insertIgnore(part);
        } catch (Exception e) {
            if (!isDuplicateKeyException(e)) {
                return FileResult.error("保存分片元数据失败");
            }
            // 重复键异常忽略，表示已存在
        }
        return FileResult.success();
    }

    /**
     * 获取已上传分片
     */
    public FileResult<List<Integer>> getUploadedParts(String uploadId) {
        CosFile file = this.getOne(new LambdaQueryWrapper<CosFile>().eq(CosFile::getUploadId, uploadId));
        if (file == null) {
            return FileResult.error("上传记录不存在");
        }

        List<Integer> parts = cosFilePartMapper.selectList(
                new LambdaQueryWrapper<CosFilePart>().eq(CosFilePart::getFileId, file.getId())
        ).stream().map(CosFilePart::getPartNumber).collect(Collectors.toList());

        return FileResult.success(parts);
    }

    /**
     * 完成分片上传
     */
    public FileResult<String> completeMultipartUpload(String uploadId) {
        CosFile file = this.getOne(new LambdaQueryWrapper<CosFile>().eq(CosFile::getUploadId, uploadId));
        if (file == null) {
            return FileResult.error("上传记录不存在");
        }
    
        List<CosFilePart> parts = cosFilePartMapper.selectList(
                new LambdaQueryWrapper<CosFilePart>().eq(CosFilePart::getFileId, file.getId())
        );
    
        if (parts.isEmpty()) {
            return FileResult.error("无分片数据");
        }

        // 检查分片数量是否匹配
        if (parts.size() != file.getTotalParts()) {
            return FileResult.error(String.format("分片上传不完整，期望 %d 个分片，实际 %d 个",
                    file.getTotalParts(), parts.size()));
        }

        // 检查 PartNumber 是否连续
        Set<Integer> partNumbers = parts.stream()
                .map(CosFilePart::getPartNumber)
                .collect(Collectors.toSet());

        for (int i = 1; i <= file.getTotalParts(); i++) {
            if (!partNumbers.contains(i)) {
                return FileResult.error(String.format("缺少第 %d 个分片", i));
            }
        }

        // 检查 ETag 是否为空
        for (CosFilePart part : parts) {
            if (part.getETag() == null || part.getETag().trim().isEmpty()) {
                return FileResult.error(String.format("第 %d 个分片的 ETag 为空", part.getPartNumber()));
            }
        }
        // =========================================
    
        List<PartETag> partETags = parts.stream()
                .map(p -> new PartETag(p.getPartNumber(), p.getETag()))
                .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                .collect(Collectors.toList());
    
        // 使用 objectKey
        CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                bucketName, file.getObjectKey(), uploadId, partETags);
    
        try {
            CompleteMultipartUploadResult result = cosClient.completeMultipartUpload(req);
            return updateFileStatusAndCleanParts(file, result.getLocation());
        } catch (Exception e) {
            file.setStatus(STATUS_FAILED);
            file.setUniqHash(null); // 清空唯一约束字段
            this.updateById(file);
            return FileResult.error("合并失败：" + e.getMessage());
        }
    }

    /**
     * 更新文件状态并清理分片数据
     */
    @Transactional
    protected FileResult<String> updateFileStatusAndCleanParts(CosFile file, String url) {
        file.setStatus(CosFile.STATUS_COMPLETED);
        file.setUrl(url);
        file.setUniqHash(null);   // 清空唯一约束字段
        this.updateById(file);
        cosFilePartMapper.delete(new LambdaQueryWrapper<CosFilePart>().eq(CosFilePart::getFileId, file.getId()));
        return FileResult.success(url);
    }

    /**
     * 判断是否为重复键异常
     */
    private boolean isDuplicateKeyException(Exception e) {
        String msg = e.getMessage().toLowerCase();
        return msg.contains("duplicate") || msg.contains("unique") || msg.contains("uk_");
    }

    /**
     * 刷新临时密钥（用于前端上传超时后重新获取凭证）
     * @param uploadId 上传会话ID
     * @return 新的临时凭证
     */
    public FileResult<Map<String, Object>> refreshCredentials(String uploadId) {
        CosFile file = this.getOne(new LambdaQueryWrapper<CosFile>().eq(CosFile::getUploadId, uploadId));
        if (file == null) {
            return FileResult.error("上传记录不存在");
        }
        if (!Arrays.asList(STATUS_INIT, STATUS_UPLOADING).contains(file.getStatus())) {
            return FileResult.error("当前状态不允许刷新凭证：" + file.getStatus());
        }

        // 重新生成临时密钥（沿用之前的 objectKey）
        Map<String, Object> credentials = stsService.getTempCredential(file.getObjectKey(), null);

        Map<String, Object> result = new HashMap<>();
        result.put("credentials", credentials);
        result.put("bucket", bucketName);
        result.put("region", region);
        result.put("objectKey", file.getObjectKey());
        result.put("uploadId", uploadId);

        return FileResult.success(result);
    }

    /**
     * 定时清理超过24小时未完成的上传任务
     * 每天凌晨 2:00 执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredUploads() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);

        // 分页查询，每次处理 100 条
        int pageSize = 100;
        int currentPage = 0;
        boolean hasMore = true;

        while (hasMore) {
            // 查询过期且未完成的记录
            LambdaQueryWrapper<CosFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CosFile::getStatus, Arrays.asList(STATUS_INIT, STATUS_UPLOADING, STATUS_FAILED))
                    .lt(CosFile::getCreateTime, expireTime)
                    .last("LIMIT " + (currentPage * pageSize) + "," + pageSize);

            List<CosFile> expiredFiles = this.list(wrapper);
            if (expiredFiles.isEmpty()) {
                hasMore = false;
                break;
            }

            for (CosFile file : expiredFiles) {
                cleanSingleExpiredUpload(file);
            }

            currentPage++;
            // 如果本次查询数量小于 pageSize，说明已经是最后一批
            if (expiredFiles.size() < pageSize) {
                hasMore = false;
            }
        }
    }

    /**
     * 清理单个过期上传：取消 COS 分片上传，删除或标记记录
     */
    @Transactional
    protected void cleanSingleExpiredUpload(CosFile file) {
        try {
            // 1. 调用 COS 取消分片上传（如果 uploadId 存在）
            if (file.getUploadId() != null && !file.getUploadId().isEmpty()) {
                AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                        bucketName, file.getObjectKey(), file.getUploadId());
                cosClient.abortMultipartUpload(abortRequest);
            }

            // 2. 删除关联的分片记录
            cosFilePartMapper.delete(new LambdaQueryWrapper<CosFilePart>().eq(CosFilePart::getFileId, file.getId()));

            // 3. 方案一：物理删除文件记录
            this.removeById(file.getId());

            // 方案二：标记为过期（可选，根据需要选择）
            // file.setStatus(STATUS_EXPIRED);
            // this.updateById(file);

        } catch (Exception e) {
            log.error("清理过期上传失败, fileId={}, objectKey={}", file.getId(), file.getObjectKey(), e);
            // 不抛出异常，避免影响其他记录的处理
        }
    }


}