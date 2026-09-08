package com.guvi.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Render (like Heroku) exposes a managed Postgres as a single DATABASE_URL in the
 * form {@code postgres://user:password@host:port/database}. Spring needs a
 * {@code jdbc:postgresql://host:port/database} URL plus the credentials supplied
 * separately, and nothing in render.yaml can perform that transformation, so it is
 * done here at startup.
 *
 * <p>An explicitly configured DB_URL always wins — this only fills in when the
 * database was wired up automatically by the platform.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "platform-database-url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicitUrl = environment.getProperty("DB_URL");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return; // operator supplied a JDBC URL directly; leave it alone
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return; // already a JDBC URL, or a scheme we don't translate
        }

        Map<String, Object> properties = translate(databaseUrl);
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    /** Visible for testing. */
    static Map<String, Object> translate(String databaseUrl) {
        URI uri = URI.create(databaseUrl);

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost())
                .append(':')
                .append(port)
                .append('/')
                .append(database);
        String query = jdbcSafeQuery(uri.getQuery());
        if (!query.isEmpty()) {
            jdbcUrl.append('?').append(query);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", jdbcUrl.toString());
        properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int separator = userInfo.indexOf(':');
            String username = separator < 0 ? userInfo : userInfo.substring(0, separator);
            String password = separator < 0 ? "" : userInfo.substring(separator + 1);
            properties.put("spring.datasource.username", decode(username));
            properties.put("spring.datasource.password", decode(password));
        }
        return properties;
    }

    /**
     * Managed Postgres providers hand out libpq-style connection strings, and some of
     * those parameters have no JDBC equivalent — Neon, for example, appends
     * {@code channel_binding=require}. Passing them through to the pgjdbc driver risks
     * failing the connection, so they are dropped here. Recognised parameters such as
     * {@code sslmode} are preserved, because managed providers require them.
     */
    private static String jdbcSafeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        for (String param : query.split("&")) {
            if (param.isBlank()) {
                continue;
            }
            String name = param.contains("=") ? param.substring(0, param.indexOf('=')) : param;
            if (LIBPQ_ONLY_PARAMETERS.contains(name.toLowerCase())) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append('&');
            }
            kept.append(param);
        }
        return kept.toString();
    }

    private static final java.util.Set<String> LIBPQ_ONLY_PARAMETERS = java.util.Set.of(
            "channel_binding",
            "target_session_attrs",
            "sslnegotiation",
            "gssencmode");

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
