package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SlSchoolMapper extends BaseMapper<SlSchool> {
    @Select("SELECT * FROM sl_school WHERE allow = 1 LIMIT 1")
    SlSchool selectDefaultEnabled();
}
