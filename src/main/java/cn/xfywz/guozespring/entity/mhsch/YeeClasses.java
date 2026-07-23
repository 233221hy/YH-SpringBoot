package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级
 * @TableName yee_classes
 */
@TableName(value ="yee_classes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeClasses {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 所属学院
     */
    private Integer collegeId;

    /**
     * 是否启用
     */
    private Integer allow;

    /**
     * 创建时间
     */
    private Date addTime;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 
     */
    private Date addDate;

    /**
     * 学生年级
     */
    private Integer entryYear;

    //班级人数
    private Integer count;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        YeeClasses other = (YeeClasses) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getCollegeId() == null ? other.getCollegeId() == null : this.getCollegeId().equals(other.getCollegeId()))
            && (this.getAllow() == null ? other.getAllow() == null : this.getAllow().equals(other.getAllow()))
            && (this.getAddTime() == null ? other.getAddTime() == null : this.getAddTime().equals(other.getAddTime()))
            && (this.getSchoolId() == null ? other.getSchoolId() == null : this.getSchoolId().equals(other.getSchoolId()))
            && (this.getAddDate() == null ? other.getAddDate() == null : this.getAddDate().equals(other.getAddDate()))
            && (this.getEntryYear() == null ? other.getEntryYear() == null : this.getEntryYear().equals(other.getEntryYear()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getCollegeId() == null) ? 0 : getCollegeId().hashCode());
        result = prime * result + ((getAllow() == null) ? 0 : getAllow().hashCode());
        result = prime * result + ((getAddTime() == null) ? 0 : getAddTime().hashCode());
        result = prime * result + ((getSchoolId() == null) ? 0 : getSchoolId().hashCode());
        result = prime * result + ((getAddDate() == null) ? 0 : getAddDate().hashCode());
        result = prime * result + ((getEntryYear() == null) ? 0 : getEntryYear().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", collegeId=").append(collegeId);
        sb.append(", allow=").append(allow);
        sb.append(", addTime=").append(addTime);
        sb.append(", schoolId=").append(schoolId);
        sb.append(", addDate=").append(addDate);
        sb.append(", entryYear=").append(entryYear);
        sb.append("]");
        return sb.toString();
    }
}