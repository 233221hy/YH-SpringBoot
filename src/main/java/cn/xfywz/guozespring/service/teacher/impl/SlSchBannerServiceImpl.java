package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhmain.SlSchoolBanner;
import cn.xfywz.guozespring.entity.dto.SlSchBannerQueryParam;
import cn.xfywz.guozespring.mapper.SlSchBannerMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.SlSchBannerService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlSchBannerServiceImpl implements SlSchBannerService {

    @Autowired
    private SlSchBannerMapper slSchBannerMapper;

    /**
     * 获取Banner列表
     * @param pageSize
     * @param pageNum
     * @param schoolId
     * @return
     */
    @Override
    public Result list(int pageSize, int pageNum, Integer schoolId) {
        Page<SlSchoolBanner> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SlSchoolBanner> queryWrapper = new QueryWrapper<>();

        // 仅当 schoolId > 0 时才添加学校筛选条件
        if (schoolId > 0) {
            queryWrapper.eq("schoolId", schoolId);
        }

        queryWrapper.orderByDesc("sort"); // 按 sort 字段降序排序
        slSchBannerMapper.selectPage(page, queryWrapper);

        return Result.success(page.getRecords(), page.getTotal());
    }

//    /**
//     * 获取Banner列表(需要登陆)
//     * @param pageSize
//     * @param pageNum
//     * @return
//     */
//    @Override
//    public Result selectAll(int pageSize, int pageNum) {
//        //根据学校id查询Banner
//        Page<SlSchoolBanner> page = new Page<>(pageNum, pageSize);
//        Page<SlSchoolBanner> pageResult = slSchBannerMapper.selectPage(page, new QueryWrapper<SlSchoolBanner>().orderByDesc("sort"));
//        return pageResult.getTotal() > 0 ? Result.success(pageResult.getRecords(), pageResult.getTotal()) : Result.error("未找到对应Banner信息");
//    }



    @Override
    public Result info(long id, int schoolId) {
        QueryWrapper<SlSchoolBanner> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id).eq("schoolId", schoolId);
        SlSchoolBanner banner = slSchBannerMapper.selectOne(queryWrapper);
        return banner != null ? Result.success(banner) : Result.error("未找到对应Banner信息");
    }


    @Override
    public Result add(SlSchoolBanner slSchBanner) {
        // 验证image字段长度不超过100个字符
        if (slSchBanner.getImage() != null && slSchBanner.getImage().length() > 255) {
            return Result.error("图片链接长度不能超过255个字符");
        }
        
        int insert = slSchBannerMapper.insert(slSchBanner);
        return insert > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }


    @Override
    public Result update(SlSchoolBanner slSchBanner) {
        // 验证image字段长度不超过100个字符
        if (slSchBanner.getImage() != null && slSchBanner.getImage().length() > 255) {
            return Result.error("图片链接长度不能超过255个字符");
        }
        
        int update = slSchBannerMapper.updateById(slSchBanner);
        return update > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slSchBannerMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result searchByCondition(SlSchBannerQueryParam param) {
        int search = Math.toIntExact(slSchBannerMapper.selectCount(new QueryWrapper<SlSchoolBanner>()));
        return search > 0 ? Result.success(slSchBannerMapper.selectList(new QueryWrapper<SlSchoolBanner>())) : Result.error("未找到对应Banner信息");
    }




}
