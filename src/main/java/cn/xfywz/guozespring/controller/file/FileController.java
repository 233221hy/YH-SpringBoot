package cn.xfywz.guozespring.controller.file;

import cn.xfywz.guozespring.entity.dto.MultipartInitResponse;
import cn.xfywz.guozespring.service.file.FileService;
import cn.xfywz.guozespring.util.FileResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("apiFileController")
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    @PostMapping("/init")
    public FileResult<MultipartInitResponse> initUpload(
            @RequestParam String fileName,
            @RequestParam @NotNull(message = "文件大小不能为空") @Min(value = 1, message = "文件大小不能为 0 字节") Long fileSize,
            @RequestParam String fileHash) {
        return fileService.initMultipartUpload(fileName, fileSize, fileHash);
    }

//    @PostMapping("/upload-part")
//    public FileResult<Void> uploadPart(
//            @RequestParam String uploadId,
//            @RequestParam Integer partNumber,
//            @RequestParam MultipartFile chunk) {
//        return fileService.uploadPart(uploadId, partNumber, chunk);
//    }

    @PostMapping("/report-part")
    public FileResult<Void> reportPart(
            @RequestParam String uploadId,
            @RequestParam Integer partNumber,
            @RequestParam String eTag) {
        return fileService.reportPart(uploadId, partNumber, eTag);
    }

    @GetMapping("/uploaded-parts")
    public FileResult<List<Integer>> getUploadedParts(@RequestParam String uploadId) {
        return fileService.getUploadedParts(uploadId);
    }

    @PostMapping("/complete")
    public FileResult<String> completeUpload(@RequestParam String uploadId) {
        return fileService.completeMultipartUpload(uploadId);
    }

    /**
     * 刷新上传凭证
     *
     * @param uploadId 上传任务ID
     * @return 刷新后的凭证
     */
    @PostMapping("/refresh")
    public FileResult<Map<String, Object>> refreshCredentials(@RequestParam String uploadId) {
        return fileService.refreshCredentials(uploadId);
    }
}