package edu.bookpict.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // postgresql://user:pass@host:port/dbname -> jdbc:postgresql://host:port/dbname
        URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
        String[] userInfo = uri.getUserInfo().split(":");

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(userInfo[0]);
        ds.setPassword(userInfo[1]);
        ds.setMaximumPoolSize(5);
        ds.setDriverClassName("org.postgresql.Driver");

        return ds;
    }
}
