package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SlOpenNodeMapper extends BaseMapper<SlOpenNode> {
    // 根据 chapterIds 查询
    @Select("<script>" +
            "SELECT * FROM sl_open_node " +
            "WHERE chapterId IN " +
            "<foreach collection='chapterIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SlOpenNode> selectByChapterIds(@Param("chapterIds") Set<Long> chapterIds);
}
