package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.exception.DatabaseException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
@NoArgsConstructor
public class YeeCollegeVO {
    private Integer id;
    private String name;
    private Integer allow;

    public static YeeCollegeVO fromResultSet(ResultSet rs) {
        YeeCollegeVO vo = new YeeCollegeVO();
        try {
            vo.setId(rs.getInt("id"));
            vo.setName(rs.getString("name"));
            vo.setAllow(rs.getInt("allow"));
        } catch (SQLException e) {
            throw new DatabaseException("映射学院数据失败", e);
        }
        return vo;
    }
}
