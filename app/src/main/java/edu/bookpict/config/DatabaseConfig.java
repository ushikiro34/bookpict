package edu.bookpict.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Slf4j
@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        String pgUser = System.getenv("PGUSER");
        String pgPassword = System.getenv("PGPASSWORD");

        log.info("DATABASE_URL present: {}, PGUSER present: {}", databaseUrl != null, pgUser != null);

        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // jdbc: prefix 제거
        if (databaseUrl.startsWith("jdbc:")) {
            databaseUrl = databaseUrl.substring(5);
        }

        // postgres:// -> postgresql:// 정규화
        if (databaseUrl.startsWith("postgres://")) {
            databaseUrl = "postgresql" + databaseUrl.substring("postgres".length());
        }

        URI uri = URI.create(databaseUrl);

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        // user/password: URL에 포함되어 있으면 사용, 없으면 PGUSER/PGPASSWORD 사용
        String username;
        String password;
        if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
            String[] userInfo = uri.getUserInfo().split(":");
            username = userInfo[0];
            password = userInfo[1];
        } else {
            username = pgUser;
            password = pgPassword;
        }

        log.info("JDBC URL: jdbc:postgresql://{}:{}{}, user: {}", uri.getHost(), uri.getPort(), uri.getPath(), username);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(5);
        ds.setDriverClassName("org.postgresql.Driver");

        return ds;
    }
}
