package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.VideoRank;
import cn.xfywz.guozespring.util.Result;

public interface VideoRankService {
    /**
     * 视频播放排行榜（按节点/视频维度汇总时长与播放量）
     */
    Result list(VideoRank param) throws Exception;
}
