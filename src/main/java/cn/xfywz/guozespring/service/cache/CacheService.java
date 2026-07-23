package cn.xfywz.guozespring.service.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface CacheService {

    /**
     * 从缓存获取，若不存在则执行 loader 并存入缓存
     * @param key       缓存键
     * @param loader    数据加载逻辑
     * @param timeout   过期时间
     * @param unit      时间单位
     * @param <T>       返回值类型
     */
    <T> T getOrLoad(String key, Supplier<T> loader, long timeout, TimeUnit unit);

    /**
     * 删除缓存
     */
    void evict(String key);

    /**
     * 批量删除（按前缀匹配）
     */
    void evictByPrefix(String prefix);
}