package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlSchoolBanner;
import cn.xfywz.guozespring.service.teacher.SlSchBannerService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.CosClientUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/manage")
public class SlSchBannerController {

    @Autowired
    private SlSchBannerService slSchBannerService;

    @GetMapping("/sl_sch_banner_list")
    public Result list(@RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "0") Integer schoolId) {
        return slSchBannerService.list(pageSize, pageNum, schoolId);
    }

    //添加轮播图（multipart，支持图片上传）
    @PostMapping(value = "/sl_sch_banner_add", consumes = "multipart/form-data")
    public Result addWithUpload(@RequestParam String name,
                                @RequestParam(required = false) String link,
                                @RequestParam(defaultValue = "1") long allow,
                                @RequestParam(defaultValue = "0") Integer schoolId,
                                @RequestParam(required = false) Integer sort,
                                @RequestPart(value = "image", required = false) MultipartFile image,
                                @RequestParam(required = false) String imageUrl) {
        SlSchoolBanner banner = new SlSchoolBanner();
        banner.setName(name);
        banner.setLink(link);
        banner.setAllow(allow);
        banner.setSchoolId(schoolId);
        banner.setSort(sort);
        if (image != null && !image.isEmpty()) {
            String url = CosClientUtil.upload(image);
            banner.setImage(url);
        }else if (imageUrl != null && !imageUrl.isBlank()) {
            banner.setImage(imageUrl);
        }
        return slSchBannerService.add(banner);
    }

    //修改轮播图（JSON）
    @PostMapping("/sl_sch_banner_update")
    public Result update(@RequestBody SlSchoolBanner slSchBanner) throws Exception {
            return slSchBannerService.update(slSchBanner);
    }

    //删除轮播图
    @GetMapping("/sl_sch_banner_delete")
    public Result delete(@RequestParam Integer id) throws Exception {
            return slSchBannerService.delete(id);
    }
}
