package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SlTplNodeMapper extends BaseMapper<SlTplNode> {

    // 根据 chapterIds 查询
    @Select("<script>" +
            "SELECT * FROM sl_tpl_node " +
            "WHERE chapterId IN " +
            "<foreach collection='chapterIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SlTplNode> selectByChapterIds(@Param("chapterIds") Set<Long> chapterIds);
}