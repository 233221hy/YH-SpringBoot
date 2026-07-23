package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseNodeMapper {
    //公开课章节
    @Select("select * from sl_open_chapter where courseId=#{id}")
    List<SlOpenChapter> getChapterName(int id);
    @Select("select * from sl_open_node where chapterId=#{id}")
    List<SlOpenNode> getCourseNode(int id);

    //全部课程章节
    @Select("select * from sl_tpl_chapter where courseId=#{id}")
    List<SlTplChapter> getTplChapters(int id);
    @Select("select * from sl_tpl_node WHERE chapterId=#{id}")
    List<SlTplNode> getTplNodes(int id);
}
