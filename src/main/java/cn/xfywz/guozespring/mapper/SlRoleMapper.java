package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlAuthNode;
import cn.xfywz.guozespring.entity.mhmain.SlRole;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlRoleMapper extends BaseMapper<SlRole> {
    @Select("select * from sl_auth_node")
    List<SlAuthNode> selectAllNode();

    @Select("select * from sl_role where id=#{role} and schoolId=#{schoolId}")
    SlRole selectByIdAndSchoolId(long role, long schoolId);

}
