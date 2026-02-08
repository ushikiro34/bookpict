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
        log.info("DATABASE_URL present: {}, length: {}",
                databaseUrl != null, databaseUrl != null ? databaseUrl.length() : 0);

        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // jdbc: prefix가 이미 붙어있으면 제거
        if (databaseUrl.startsWith("jdbc:")) {
            databaseUrl = databaseUrl.substring(5);
        }

        // postgres:// -> postgresql:// 정규화
        if (databaseUrl.startsWith("postgres://")) {
            databaseUrl = "postgresql" + databaseUrl.substring("postgres".length());
        }

        log.info("Parsed URL scheme: {}", databaseUrl.substring(0, Math.min(15, databaseUrl.length())));

        URI uri = URI.create(databaseUrl);

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
        String[] userInfo = uri.getUserInfo().split(":");

        log.info("JDBC URL: jdbc:postgresql://{}:{}{}", uri.getHost(), uri.getPort(), uri.getPath());

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(userInfo[0]);
        ds.setPassword(userInfo[1]);
        ds.setMaximumPoolSize(5);
        ds.setDriverClassName("org.postgresql.Driver");

        return ds;
    }
}
