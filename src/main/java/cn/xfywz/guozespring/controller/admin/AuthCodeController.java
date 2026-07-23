package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.vo.AuthCode;
import cn.xfywz.guozespring.util.CodeImgUtil;
import cn.xfywz.guozespring.util.RedisUtils;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/")
public class AuthCodeController {
    @Autowired
    RedisUtils redisUtils;

    private String convertToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("图片编码失败", e);
        }
    }
    @GetMapping("/captcha")
    public Result getCaptcha() {
        // 生成默认验证码
        Map<String, Object> captcha = CodeImgUtil.generateCode(4, 200, 50, CodeImgUtil.CodeType.MIX);
        String code = (String) captcha.get("code");
        BufferedImage image = (BufferedImage) captcha.get("image");
        // 存储验证码到缓存（设置5分钟过期）
        String uuid = UUID.randomUUID().toString();
        // 将图片转为base64
        String base64Image = convertToBase64(image);
        redisUtils.set(uuid, code, 5 * 60);
        // 返回JSON响应（包含验证码ID和base64图片）
        return Result.success(uuid, base64Image);
    }
    @PostMapping("/verify")
    public Result verify(AuthCode authCode) {
        // 获取缓存中的验证码
        String redisCode = (String) redisUtils.get(authCode.getUuid());
        // 验证码校验
        if (redisCode != null && redisCode.equalsIgnoreCase(authCode.getCode())){
            return Result.success("验证成功");
        }else return Result.error("验证码错误", "验证码不匹配");
    }
}
