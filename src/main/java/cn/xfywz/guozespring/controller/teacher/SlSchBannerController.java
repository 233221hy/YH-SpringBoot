package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhmain.SlSchoolBanner;
import cn.xfywz.guozespring.entity.dto.SlSchBannerQueryParam;
import cn.xfywz.guozespring.service.teacher.SlSchBannerService;
import cn.xfywz.guozespring.util.CosClientUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequireAuth
@RestController("teaSlSchBannerController")
@RequestMapping("/school")
public class SlSchBannerController {

    @Autowired
    private SlSchBannerService slSchBannerService;


    //根据id查询轮播图
    @GetMapping("/sl_sch_banner_info")
    public Result info(@RequestParam long id,
                      @RequestParam int schoolId) throws Exception {
        return slSchBannerService.info(id,schoolId);
    }

    //添加轮播图（JSON）
    @PostMapping("/sl_sch_banner_add")
    public Result add(@RequestBody SlSchoolBanner slSchBanner) throws Exception {
        return slSchBannerService.add(slSchBanner);
    }

    //添加轮播图（multipart，支持图片上传）
    @PostMapping(value = "/sl_sch_banner_add", consumes = "multipart/form-data")
    public Result addWithUpload(@RequestParam String name,
                                @RequestParam(required = false) String link,
                                @RequestParam(defaultValue = "1") long allow,
                                @RequestParam int schoolId,
                                @RequestParam(required = false) Integer sort,
                                @RequestPart(value = "image", required = false) MultipartFile image,
                                @RequestParam(required = false) String imageUrl) throws Exception {
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

    //修改轮播图（multipart，支持图片上传）
    @PostMapping(value = "/sl_sch_banner_update", consumes = "multipart/form-data")
    public Result updateWithUpload(@RequestParam long id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String link,
                                   @RequestParam(defaultValue = "1") long allow,
                                   @RequestParam int schoolId,
                                   @RequestParam(required = false) Integer sort,
                                   @RequestPart(value = "image", required = false) MultipartFile image,
                                   @RequestParam(required = false) String imageUrl) throws Exception {
        SlSchoolBanner banner = new SlSchoolBanner();
        banner.setId(id);
        banner.setName(name);
        banner.setLink(link);
        banner.setAllow(allow);
        banner.setSchoolId(schoolId);
        banner.setSort(sort);
        if (image != null && !image.isEmpty()) {
            String url = CosClientUtil.upload(image);
            banner.setImage(url);
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            banner.setImage(imageUrl);
        }
        return slSchBannerService.update(banner);
    }

    //删除轮播图
    @GetMapping("/sl_sch_banner_delete")
    public Result delete(@RequestParam Integer id,
                         @RequestParam int schoolId) throws Exception {
        return slSchBannerService.delete(id);
    }

    //条件查询
    @PostMapping("/sl_sch_banner_search")
    public Result search(@RequestBody SlSchBannerQueryParam param) throws Exception {
        return slSchBannerService.searchByCondition(param);
    }
}
