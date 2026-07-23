package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpinionLike {
    Integer type;
    Timestamp startTime;
    Timestamp endTime;
    Integer PageSize;
    Integer PageNum;
    void setStartTime(Long s){
        this.startTime= new Timestamp(s);
    }
    void setEndTime(Long s){
        this.endTime= new Timestamp(s);
    }
}
