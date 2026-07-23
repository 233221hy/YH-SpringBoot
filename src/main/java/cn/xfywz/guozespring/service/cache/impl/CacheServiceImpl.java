package cn.xfywz.guozespring.service.cache.impl;

import cn.xfywz.guozespring.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T getOrLoad(String key, Supplier<T> loader, long timeout, TimeUnit unit) {
        // 1. 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("缓存命中: {}", key);
            return (T) cached;
        }

        // 2. 缓存未命中，执行实际加载逻辑
        log.debug("缓存未命中，加载数据: {}", key);
        T value = loader.get();
        if (value != null) {
            // 3. 存入缓存
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("数据已缓存: {}, TTL={} {}", key, timeout, unit);
        }
        return value;
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        log.debug("缓存已删除: {}", key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("批量删除缓存: {} ({} 条)", prefix, keys.size());
        }
    }
}