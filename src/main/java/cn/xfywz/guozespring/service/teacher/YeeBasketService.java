package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeBasket;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.util.Result;

public interface YeeBasketService {
    Result selectAll(int schoolId, Integer userId) throws Exception;
    Result add(YeeBasket yeeBasket) throws Exception;
    Result delete(int schoolId, int id, Integer userId) throws Exception;

    Result deleteAll(int schoolId, int userId) throws Exception;
}
