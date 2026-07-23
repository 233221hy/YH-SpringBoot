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
public class YeeSchoolColumnVO {
    private Integer id;
    private Integer schoolId;
    private String name;
    private Integer type;
    private Integer allow;
    private Integer sort;
    private Timestamp addTime;
    private String data;
    private String more;

    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeSchoolColumnVO fromResultSet(ResultSet rs){
        try {
            YeeSchoolColumnVO vo = new YeeSchoolColumnVO();
            vo.setId(rs.getInt("id"));
            vo.setSchoolId(rs.getInt("schoolId"));
            vo.setName(rs.getString("name"));
            vo.setType(rs.getInt("type"));
            vo.setAllow(rs.getInt("allow"));
            vo.setSort(rs.getInt("sort"));
            vo.setAddTime(rs.getTimestamp("addTime"));
            vo.setData(rs.getString("data"));
            vo.setMore(rs.getString("more"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("映射学校栏目列表失败", e);
        }
    }

}
