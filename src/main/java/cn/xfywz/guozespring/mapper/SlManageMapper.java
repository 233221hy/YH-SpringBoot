package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlManageMapper extends BaseMapper<SlManage> {
    @Select("select * from sl_manage where account=#{username}")
    SlManage ShowAccount(String username);
    @Select("select password from sl_manage where id=#{id}")
    String selectOldPwd(long id);
    @Select("select * from sl_manage where name like #{name}")
    List<SlManage> selectLikeName(String name);
    @Select("select * from sl_manage where account like #{account}")
    List<SlManage> selectLikeAccount(String account);
}
