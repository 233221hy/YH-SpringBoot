package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ConnectionCleanupInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 请求结束 → 强制回收连接
        SlaveMysqlConnectionUtil.cleanup();
    }
}
