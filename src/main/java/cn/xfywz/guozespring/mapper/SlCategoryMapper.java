package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlCategoryMapper extends BaseMapper<SlCategory> {
    @Select("select * from sl_category where pid=0 and allow=1")
    List<SlCategory> selectAll();

    @Select("select * from sl_category where pid=#{id} and allow=1")
    List<SlCategory> selectById(long id);
}
