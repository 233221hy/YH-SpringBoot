package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhsch.YeeRoleAuth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface YeeRoleAuthMapper extends BaseMapper<YeeRoleAuth> {

    @Select("SELECT * FROM yee_role_auth WHERE roleId = #{roleId} AND schoolId = #{schoolId}")
    List<YeeRoleAuth> selectByRoleIdAndSchoolId(@Param("roleId") long roleId, @Param("schoolId") long schoolId);

    @Select("SELECT * FROM yee_role_auth WHERE authId = #{authId} AND schoolId = #{schoolId}")
    List<YeeRoleAuth> selectByAuthIdAndSchoolId(@Param("authId") long authId, @Param("schoolId") long schoolId);

    @Delete("DELETE FROM yee_role_auth WHERE roleId = #{roleId} AND schoolId = #{schoolId}")
    int deleteByRoleIdAndSchoolId(@Param("roleId") long roleId, @Param("schoolId") long schoolId);

    @Delete("DELETE FROM yee_role_auth WHERE roleId = #{roleId} AND authId = #{authId} AND schoolId = #{schoolId}")
    int deleteByRoleIdAndAuthIdAndSchoolId(@Param("roleId") long roleId, @Param("authId") long authId, @Param("schoolId") long schoolId);
}