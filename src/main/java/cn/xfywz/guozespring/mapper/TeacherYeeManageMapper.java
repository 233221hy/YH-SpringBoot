package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeacherYeeManageMapper extends BaseMapper<YeeManage> {
    /**
     * 根据账号查询教师信息
     * @param account 账号
     * @return 教师信息
     */
    @Select("select * from yee_manage where account = #{account}")
    YeeManage selectByAccount(@Param("account") String account);
}
