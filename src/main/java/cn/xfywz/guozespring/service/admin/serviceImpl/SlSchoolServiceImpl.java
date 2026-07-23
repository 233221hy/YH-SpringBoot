package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.SlSchoolService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class SlSchoolServiceImpl implements SlSchoolService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Override
    public Result selectAll() {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        List<SlSchool> slSchool = slSchoolMapper.selectList(queryWrapper);
        return Result.success((Object) slSchool, Long.valueOf((long) slSchool.size()));
    }

    @Override
    public Result add(SlSchool slSchool) {
        slSchool.setAddTime(new Timestamp(System.currentTimeMillis()));
        if (slSchoolMapper.insert(slSchool)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlSchool slSchool) {
        if (slSchool == null) {
            return Result.error("参数错误：学校ID不能为空");
        }

        UpdateWrapper<SlSchool> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", slSchool.getId());

        // 只更新前端可能传的业务字段（跳过 dbHost/dbPort 等敏感字段）
        if (slSchool.getName() != null && !slSchool.getName().trim().isEmpty()) {
            updateWrapper.set("name", slSchool.getName());
        }
        if (slSchool.getNameEn() != null) {
            updateWrapper.set("name_en", slSchool.getNameEn());
        }
        if (slSchool.getIdent() != null) {
            updateWrapper.set("ident", slSchool.getIdent());
        }
        if (slSchool.getArea() != null) {
            updateWrapper.set("area", slSchool.getArea());
        }
        if (slSchool.getProvince() != null && slSchool.getProvince() != 0) {
            updateWrapper.set("province", slSchool.getProvince());
        }
        if (slSchool.getCity() != null && slSchool.getCity() != 0) {
            updateWrapper.set("city", slSchool.getCity());
        }
        if (slSchool.getRegion() != null && slSchool.getRegion() != 0) {
            updateWrapper.set("region", slSchool.getRegion());
        }
        if (slSchool.getBadge() != null) {
            updateWrapper.set("badge", slSchool.getBadge());
        }
        if (slSchool.getLogo() != null) {
            updateWrapper.set("logo", slSchool.getLogo());
        }
        if (slSchool.getAddress() != null) {
            updateWrapper.set("address", slSchool.getAddress());
        }
        if (slSchool.getWebsite() != null) {
            updateWrapper.set("website", slSchool.getWebsite());
        }
        if (slSchool.getIntro() != null) {
            updateWrapper.set("intro", slSchool.getIntro());
        }
        if (slSchool.getAllow() != null && slSchool.getAllow() != 0) {
            updateWrapper.set("allow", slSchool.getAllow());
        }
        if (slSchool.getContent() != null) {
            updateWrapper.set("content", slSchool.getContent());
        }
        if (slSchool.getUseCourse() != null && slSchool.getUseCourse() != 0) {
            updateWrapper.set("useCourse", slSchool.getUseCourse());
        }
        if (slSchool.getBanner() != null) {
            updateWrapper.set("banner", slSchool.getBanner());
        }
        if (slSchool.getMap() != null) {
            updateWrapper.set("map", slSchool.getMap());
        }
        if (slSchool.getSort() != null && slSchool.getSort() != 0) {
            updateWrapper.set("sort", slSchool.getSort());
        }
        if (slSchool.getDomain() != null) {
            updateWrapper.set("domain", slSchool.getDomain());
        }
        if (slSchool.getDomainType() != null && slSchool.getDomainType() != 0) {
            updateWrapper.set("domainType", slSchool.getDomainType());
        }
        if (slSchool.getSkin() != null) {
            updateWrapper.set("skin", slSchool.getSkin());
        }
        if (slSchool.getContact() != null) {
            updateWrapper.set("contact", slSchool.getContact());
        }
        if (slSchool.getCopyright() != null) {
            updateWrapper.set("copyright", slSchool.getCopyright());
        }

        // 注意：dbHost, dbPort, dbName, dbUser, dbPass 等字段 **故意不处理**，永不更新！

        boolean updated = slSchoolMapper.update(null, updateWrapper) > 0;
        return updated ? Result.success("修改成功") : Result.error("修改失败或无变更");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slSchoolMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result selectDomain(String domain) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("domain",domain);
        SlSchool slSchool = slSchoolMapper.selectOne(queryWrapper);
        if (slSchool != null){
            return Result.success(slSchool);
        }else return Result.error("此学校不在白名单");
    }

    @Override
    public SlSchool selectById(Integer id) {
        return slSchoolMapper.selectById(id);
    }


}
