package cn.xfywz.guozespring.mapper;


import cn.xfywz.guozespring.entity.mhmain.SlSchoolBanner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SlSchBannerMapper extends BaseMapper<SlSchoolBanner> {

    @Select("select * from sl_school_banner where id=#{id}")
    public SlSchoolBanner selectById(long id);

}
