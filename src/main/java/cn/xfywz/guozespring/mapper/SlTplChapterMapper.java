package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlTplChapterMapper extends BaseMapper<SlTplChapter> {

    //id查询
    @Select("select * from sl_tpl_chapter where courseId=#{courseId}")
    List<SlTplChapter> selectByCourseId(@Param("courseId") int courseId);
}
