package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeHappyCircle;
import cn.xfywz.guozespring.service.teacher.YeeHappyCService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.CosClientUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/school")
public class YeeHappyCController {

    @Autowired
    private YeeHappyCService happyService;

    // 主评论列表（带回复数据，一次性获取）
    @GetMapping("/happy_list")
    public Result list(@RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam(required = false, defaultValue = "0") long userId,
                       @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.list(pageNum, pageSize, schoolId, userId);
        }
        return Result.error("非法访问");
    }

    // 根据id搜索
    @GetMapping("/happy_detail")
    public Result detail(@RequestParam long id,
                         @RequestParam int schoolId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.detail(schoolId, id);
        }
        return Result.error("非法访问");
    }

    // 新增主评论（JSON版，兼容旧调用）
    @PostMapping(value = "/happy_add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result add(@RequestBody YeeHappyCircle circle,
                      @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) circle.getSchoolId())) {
            return happyService.add(circle);
        }
        return Result.error("非法访问");
    }

    // 新增主评论（multipart版，支持可选附件 images/files）
    @PostMapping(value = "/happy_add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result addWithUpload(@RequestPart("circle") YeeHappyCircle circle,
                                @RequestPart(value = "images", required = false) MultipartFile[] images,
                                @RequestPart(value = "files", required = false) MultipartFile[] files,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) circle.getSchoolId())) {
            String imageUrls = buildUploadUrls(images);
            if (images != null && images.length > 0 && imageUrls == null) {
                return Result.error("图片上传失败");
            }
            if (imageUrls != null) {
                circle.setImages(imageUrls);
            }
            String fileUrls = buildUploadUrls(files);
            if (files != null && files.length > 0 && fileUrls == null) {
                return Result.error("文件上传失败");
            }
            if (fileUrls != null) {
                circle.setFiles(fileUrls);
            }
            return happyService.add(circle);
        }
        return Result.error("非法访问");
    }

    // 删除（逻辑删除）
    @GetMapping("/happy_delete")
    public Result delete(@RequestParam long id,
                         @RequestParam int schoolId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.delete(id, schoolId);
        }
        return Result.error("非法访问");
    }


    // 点赞（toggle，存在则取消，不存在则点赞，返回详细状态）
    @PostMapping("/happy_like_toggle")
    public Result likeToggle(@RequestParam int schoolId,
                             @RequestParam long replyId,
                             @RequestParam long userId,
                             @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.likeToggle(schoolId, replyId, userId);
        }
        return Result.error("非法访问");
    }

    // 新增回复（JSON版，兼容旧调用）
    @PostMapping(value = "/happy_reply_add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result addReply(@RequestBody YeeHappyCircle reply,
                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) reply.getSchoolId())) {
            return happyService.addReply(reply);
        }
        return Result.error("非法访问");
    }

    // 新增回复（multipart版，支持可选附件 images/files）
    @PostMapping(value = "/happy_reply_add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result addReplyWithUpload(@RequestPart("reply") YeeHappyCircle reply,
                                     @RequestPart(value = "images", required = false) MultipartFile[] images,
                                     @RequestPart(value = "files", required = false) MultipartFile[] files,
                                     @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) reply.getSchoolId())) {
            String imageUrls = buildUploadUrls(images);
            if (images != null && images.length > 0 && imageUrls == null) {
                return Result.error("图片上传失败");
            }
            if (imageUrls != null) {
                reply.setImages(imageUrls);
            }
            String fileUrls = buildUploadUrls(files);
            if (files != null && files.length > 0 && fileUrls == null) {
                return Result.error("文件上传失败");
            }
            if (fileUrls != null) {
                reply.setFiles(fileUrls);
            }
            return happyService.addReply(reply);
        }
        return Result.error("非法访问");
    }

    private String buildUploadUrls(MultipartFile[] fs) {
        if (fs == null || fs.length == 0) return null;
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : fs) {
            if (f == null || f.isEmpty()) continue;
            String url = CosClientUtil.upload(f);
            if (url == null || url.startsWith("上传错误")) {
                return null;
            }
            urls.add(url);
        }
        if (urls.isEmpty()) return null;
        return String.join(",", urls);
    }
}
