package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhsch.YeeRoleAuth;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import cn.xfywz.guozespring.service.admin.SlRoleService;
import cn.xfywz.guozespring.service.admin.YeeRoleAuthService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.relation.Role;

@Service
public class SlRoleServiceImpl implements SlRoleService {
    @Autowired
    private SlRoleMapper slRoleMapper;
    @Autowired
    private YeeRoleAuthService yeeRoleAuthService;
    @Override
    public Result selectAll(int schoolId, int PageSize, int PageNum) {
        Page<SlRole> page = new Page<>(PageNum, PageSize);
        QueryWrapper<SlRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schoolId",schoolId);
        Page<SlRole> page1 = slRoleMapper.selectPage(page, queryWrapper);
        return Result.success(page1.getRecords(), page1.getTotal());
    }

    @Override
    public Result add(SlRole slRole) throws Exception {
        Long schoolId = slRole.getSchoolId();
        String name = slRole.getName();
        QueryWrapper<SlRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schoolId",schoolId);
        queryWrapper.eq("name",name);
        if (slRoleMapper.selectOne(queryWrapper) != null){
            return Result.error("角色名已存在");
        }
        if (slRoleMapper.insert(slRole) > 0){
            SlRole slRole1 = slRoleMapper.selectOne(queryWrapper);
            if (schoolId==0){
                return Result.success("添加成功:");
            }else {
                YeeRoleAuth yeeRoleAuth =new YeeRoleAuth();
                yeeRoleAuth.setRoleId(slRole1.getId());
                yeeRoleAuth.setSchoolId(schoolId);
                yeeRoleAuthService.roleAuth_add(yeeRoleAuth);
                return Result.success("添加成功:");
            }

        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlRole slRole) {
        if (slRoleMapper.updateById(slRole) > 0){
            return Result.success("更新成功");
        }else return Result.error("更新失败");
    }

    @Override
    public Result delete(Integer id) {
        if (slRoleMapper.deleteById(id) > 0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }

    @Override
    public Result getNode() {
        return Result.success(slRoleMapper.selectAllNode());
    }
}
