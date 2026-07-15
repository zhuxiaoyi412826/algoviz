package com.algoviz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 读取SQL脚本
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("init-db.sql");
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // 跳过注释和空行
                    if (!line.startsWith("--") && !line.trim().isEmpty()) {
                        sqlBuilder.append(line);
                        if (line.endsWith(";")) {
                            String sql = sqlBuilder.toString();
                            statement.execute(sql);
                            sqlBuilder.setLength(0);
                        }
                    }
                }
                
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
