// CosFilePartMapper.java
package cn.xfywz.guozespring.mapper.file;

import cn.xfywz.guozespring.entity.file.CosFilePart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface CosFilePartMapper extends BaseMapper<CosFilePart> {

    @Insert("INSERT IGNORE INTO cos_file_part (file_id, part_number, e_tag, create_time) VALUES (#{fileId}, #{partNumber}, #{eTag}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertIgnore(CosFilePart part);


}