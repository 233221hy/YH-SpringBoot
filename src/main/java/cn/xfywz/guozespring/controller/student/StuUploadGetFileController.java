package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.util.CosClientUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/user")
public class StuUploadGetFileController {

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        return Result.success(CosClientUtil.upload(file));
    }
}
