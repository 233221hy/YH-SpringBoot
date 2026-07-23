package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import cn.xfywz.guozespring.entity.mhmain.SlTplNodeFiles;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
public interface SlTplNodeFilesMapper extends BaseMapper<SlTplNodeFiles> {

    @Select("<script>" +
            "SELECT * FROM sl_tpl_node_files " +
            "WHERE nodeId IN " +
            "<foreach collection='nodeIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SlTplNodeFiles> selectByNodeIds(@Param("nodeIds") Collection<Long> nodeIds);
}



