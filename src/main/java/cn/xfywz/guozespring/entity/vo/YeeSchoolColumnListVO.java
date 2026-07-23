package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeSchoolColumnListVO {
    private Integer id;
    private String name;
    private Integer type;
    private Integer allow;
    private Integer sort;
    private Timestamp addTime;

    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeSchoolColumnListVO fromResultSet(ResultSet rs){
        try {
            YeeSchoolColumnListVO vo = new YeeSchoolColumnListVO();
            vo.setId(rs.getInt("id"));
            vo.setName(rs.getString("name"));
            vo.setType(rs.getInt("type"));
            vo.setAllow(rs.getInt("allow"));
            vo.setSort(rs.getInt("sort"));
            vo.setAddTime(rs.getTimestamp("addTime"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("映射学校栏目列表失败", e);
        }

    }


}
