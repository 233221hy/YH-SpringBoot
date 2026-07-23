package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNodeFiles;
import cn.xfywz.guozespring.entity.mhmain.SlTplNodeFiles;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SlOpenNodeFilesMapper extends BaseMapper<SlOpenNodeFiles> {

    @Select("<script>" +
            "SELECT * FROM sl_open_node_files " +
            "WHERE nodeId IN " +
            "<foreach collection='nodeIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SlOpenNodeFiles> selectByNodeIds(@Param("nodeIds") Collection<Long> nodeIds);
}
