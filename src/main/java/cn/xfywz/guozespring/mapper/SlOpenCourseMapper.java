package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SlOpenCourseMapper extends BaseMapper<SlOpenCourse> {
    @Select("select id,name from sl_open_course where id=#{id}")
    SlOpenCourse selectNameById(int id);
}
