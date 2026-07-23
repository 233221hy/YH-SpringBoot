package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlOpenChapterMapper extends BaseMapper<SlOpenChapter> {
    @Select("select * from sl_open_chapter where courseId=#{courseId}")
    List<SlOpenChapter> selectByCourseId(@Param("courseId") long courseId);

}
