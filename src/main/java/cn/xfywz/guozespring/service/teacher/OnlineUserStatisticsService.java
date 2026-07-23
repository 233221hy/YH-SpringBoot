package cn.xfywz.guozespring.service.teacher;

import java.util.Map;

public interface OnlineUserStatisticsService {

    Map<String, Map<String, Integer>> statisticsTodayOnlineUserByHour();
    Map<String, Map<String, Integer>> statisticsTodayOnlineUserBy20Min();
    Map<String, Map<String, Integer>> statisticsTodayOnlineBySchool(Long schoolId);
    Map<String, Integer> getCurrentOnlineTotal();

}
