package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.mapper.SlManageMapper;
import cn.xfywz.guozespring.service.admin.SlManageService;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.Result;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;

@Service
public class SlManageServiceImpl implements SlManageService {
    @Autowired
    private SlManageMapper slManageMapper;
    BCryptPasswordEncoder bCryptPasswordEncoder=new BCryptPasswordEncoder();
    @Override
    public SlManage info(String Authorization) throws Exception {
        Claims claims = JwtTokenUtil.parseToken(Authorization);

        if (claims != null) {
            LoginUser loginUser = JSON.parseObject(claims.getSubject(), LoginUser.class);
            return loginUser.getSlManage();
        }
        return null;
    }

    @Override
    public String infoUpdate(SlManage slManage, String authorization) throws Exception {
        Claims claims = JwtTokenUtil.parseToken(authorization);
        if (claims != null){
            slManageMapper.updateById(slManage);
            return "更新成功";
        }
        return null;
    }

    @Override
    public Object infoUpdatePassword(String oldPassword,String newPassword ,String authorization) throws Exception {
        Claims claims = JwtTokenUtil.parseToken(authorization);
        if (claims != null){
            String subject = claims.getSubject();
            JSONObject json = JSON.parseObject(subject);
            SlManage slManage = json.getObject("slManage", SlManage.class);
            String oldPwd = slManageMapper.selectOldPwd(slManage.getId());
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            if (bCryptPasswordEncoder.matches(oldPassword,oldPwd)){
                slManage.setPassword(bCryptPasswordEncoder.encode(newPassword));
                slManageMapper.updateById(slManage);
                return "密码修改成功";
            }else return "旧密码错误";
        }
        return "请重新登录";
    }

    //查询管理员列表
    @Override
    public Result selectAll(int PageSize, int PageNum) {
        Page<SlManage> page = new Page<>(PageNum,PageSize);
        page = slManageMapper.selectPage(page,null);
        return Result.success(page.getRecords(),page.getTotal());
    }

    @Override
    public Result deleteById(int id) {
        slManageMapper.deleteById(id);
        return Result.success("删除成功");
    }
    @Override
    public Result addManage(SlManage slManage) {
        slManage.setPassword(bCryptPasswordEncoder.encode(slManage.getPassword()));
        SlManage slManage1 = new SlManage();
        slManage1.setAccount(slManage.getAccount());
        QueryWrapper<SlManage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account",slManage.getAccount());
        if (slManageMapper.selectOne(queryWrapper) == null){
            slManage.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
            slManage.setGeneral(1);
            slManageMapper.insert(slManage);
            return Result.success("添加成功");
        }else return Result.error("用户已存在");


    }

    @Override
    public Result updateManage(SlManage slManage) {
        if(slManage.getPassword() != null){
            slManage.setPassword(bCryptPasswordEncoder.encode(slManage.getPassword()));
            slManageMapper.updateById(slManage);
        }else slManageMapper.updateById(slManage);
        return Result.success("更新成功");
    }

    @Override
    public Result selectById(int id) {
        SlManage slManage = slManageMapper.selectById(id);
        if (slManage != null)
            return Result.success(slManage);
        else return Result.error("用户不存在");
    }

    @Override
    public Result selectLikeName(String name) {
        name="%"+name+"%";
        List<SlManage> slManages = slManageMapper.selectLikeName(name);
        return Result.success(slManages);
    }

    @Override
    public Result selectLikeAccount(String account) {
        account="%"+account+"%";
        List<SlManage> slManages = slManageMapper.selectLikeAccount(account);
        return Result.success(slManages);
    }

    @Override
    public Result manageIsLock(int id) {
        SlManage slManage = slManageMapper.selectById(id);
        if (slManage != null){
            if (slManage.getIsLock() == 0){
                slManage.setIsLock(1);
                slManageMapper.updateById(slManage);
                return Result.success("锁定成功");
            }else {
                slManage.setIsLock(0);
                slManageMapper.updateById(slManage);
                return Result.success("解锁成功");
            }
        }
        return Result.error("用户不存在");
    }


}
