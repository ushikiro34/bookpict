package edu.bookpict.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@Profile("prod")
public class DatabaseConfig {

    // postgres(ql)://user:pass@host:port/dbname
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:jdbc:)?postgres(?:ql)?://(?:([^:]+):([^@]+)@)?([^:/]+):(\\d+)/(.+)");

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        String pgHost = System.getenv("PGHOST");
        String pgPort = System.getenv("PGPORT");
        String pgDatabase = System.getenv("PGDATABASE");
        String pgUser = System.getenv("PGUSER");
        String pgPassword = System.getenv("PGPASSWORD");

        // 디버그: 어떤 변수가 있는지 확인
        log.info("=== DB Config Debug ===");
        log.info("DATABASE_URL present: {}, length: {}", databaseUrl != null, databaseUrl != null ? databaseUrl.length() : 0);
        log.info("PGHOST: [{}], PGPORT: [{}], PGDATABASE: [{}], PGUSER: [{}]", pgHost, pgPort, pgDatabase, pgUser);
        if (databaseUrl != null) {
            // 비밀번호 마스킹 후 URL 출력
            String masked = databaseUrl.replaceAll(":[^@/]+@", ":****@");
            log.info("DATABASE_URL value: {}", masked);
        }

        String jdbcUrl;
        String username;
        String password;

        // 방법1: DATABASE_URL 파싱 시도
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            Matcher m = URL_PATTERN.matcher(databaseUrl);
            if (m.matches()) {
                String host = m.group(3);
                String port = m.group(4);
                String db = m.group(5);
                username = m.group(1) != null ? m.group(1) : pgUser;
                password = m.group(2) != null ? m.group(2) : pgPassword;
                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
                log.info("Parsed from DATABASE_URL: {}, user: {}", jdbcUrl, username);
            } else {
                log.warn("DATABASE_URL does not match expected pattern, falling back to PG vars");
                jdbcUrl = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase;
                username = pgUser;
                password = pgPassword;
            }
        }
        // 방법2: 개별 PG 변수 사용
        else {
            jdbcUrl = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase;
            username = pgUser;
            password = pgPassword;
        }

        log.info("Final JDBC URL: {}, user: {}", jdbcUrl, username);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(5);
        ds.setDriverClassName("org.postgresql.Driver");

        return ds;
    }
}
