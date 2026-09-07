package com.algoviz.config;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis 慢 SQL 拦截器：
 * 拦截 StatementHandler.query/update，整段执行（含 SQL 下发与数据库往返）耗时超过
 * {@value #SLOW_MS}ms 才输出，输出落到 D:/rizi/SQL/{主机名}-slow.log（滚动）。
 * 注意：耗时为近似值（拦截点位于 JDBC Statement 前后），不统计连接获取时间。
 *
 * logback 日志类别固定为本类 FQCN（见 logback-spring.xml 中
 * com.algoviz.config.SlowSqlInterceptor logger，additivity=false，仅挂 SLOW_SQL_FILE + CONSOLE），
 * 避免慢 SQL 刷屏 INFO/WARN/DEBUG 等常规文件。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
@Component
public class SlowSqlInterceptor implements Interceptor {

    /** 慢 SQL 阈值（毫秒） */
    public static final long SLOW_MS = 100;

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startNanos = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (costMs >= SLOW_MS) {
                outputSlowSql(invocation, costMs);
            }
        }
    }

    private void outputSlowSql(Invocation invocation, long costMs) {
        try {
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = handler.getBoundSql();
            String sqlId = resolveSqlId(handler);
            String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
            String params = resolveParams(boundSql);
            String logText = String.format("[SLOW-SQL] 耗时=%dms mapper=%s params=%s sql=%s",
                    costMs, sqlId, params, sql);
            if (logText.length() > 4000) {
                logText = logText.substring(0, 4000) + "...(截断)";
            }
            log.warn(logText);
        } catch (Exception e) {
            // 兜底：解析失败不影响业务执行，仅打印耗时
            log.warn("[SLOW-SQL] 耗时={}ms，但 SQL 详情解析失败: {}", costMs, e.getMessage());
        }
    }

    /** 从代理链下层层剥壳，取出真实 MappedStatement id（形如 com.algoviz.mapper.XxxMapper.method） */
    private String resolveSqlId(StatementHandler handler) {
        try {
            MetaObject meta = SystemMetaObject.forObject(handler);
            Object ms = meta.getValue("delegate.mappedStatement");
            if (ms instanceof MappedStatement mappedStatement) {
                return mappedStatement.getId();
            }
            // 兼容多层代理：继续剥壳
            Object delegate = meta.getValue("delegate");
            if (delegate instanceof StatementHandler) {
                return resolveSqlId((StatementHandler) delegate);
            }
        } catch (Exception ignored) {
            // 取不到 mapper id 时静默
        }
        return "";
    }

    /** 参数占位 -> 参数值（仅第一个参数对象 & 内联参数映射，截断保护） */
    private String resolveParams(BoundSql boundSql) {
        try {
            List<ParameterMapping> mappings = boundSql.getParameterMappings();
            Object param = boundSql.getParameterObject();
            StringBuilder sb = new StringBuilder(128);
            if (param != null && mappings != null && !mappings.isEmpty()) {
                MetaObject meta = SystemMetaObject.forObject(param);
                sb.append("[");
                for (int i = 0; i < mappings.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    try {
                        Object v = meta.getValue(mappings.get(i).getProperty());
                        sb.append(v == null ? "null" : v.toString());
                    } catch (Exception ignored) {
                        sb.append("?");
                    }
                }
                sb.append("]");
            } else if (param != null) {
                sb.append(param.toString());
            }
            if (sb.length() > 200) {
                sb.setLength(200);
                sb.append("...(截断)");
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 预留：未来可通过 properties 配置阈值
    }
}
