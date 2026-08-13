package com.algoviz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 货币系统数据库初始化器
 * 仅在 coin.init.enabled=true 时执行（默认关闭，数据已初始化）
 * 如需重新初始化，在 application.yml 中设置 coin.init.enabled: true
 */
@Component
@ConditionalOnProperty(name = "coin.init.enabled", havingValue = "true")
@Order(2)
public class CoinDatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CoinDatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("coin_system.sql");
            if (inputStream == null) {
                logger.warn("coin_system.sql 未找到，跳过货币系统初始化");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            int executed = 0;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // 跳过注释和空行
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sqlBuilder.append(line).append("\n");
                // MySQL PREPARE/EXECUTE 块以分号结尾
                if (trimmed.endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    try {
                        statement.execute(sql);
                        executed++;
                    } catch (Exception e) {
                        logger.debug("SQL 执行跳过（可能已存在）: {}", e.getMessage());
                    }
                    sqlBuilder.setLength(0);
                }
            }
            reader.close();
            logger.info("货币系统数据库初始化完成，执行 {} 条语句", executed);
        } catch (Exception e) {
            logger.error("货币系统数据库初始化失败", e);
        }
    }
}
