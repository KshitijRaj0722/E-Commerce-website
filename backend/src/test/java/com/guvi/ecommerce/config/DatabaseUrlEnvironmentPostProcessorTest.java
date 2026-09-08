package com.guvi.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void translatesRenderStyleConnectionStringToJdbc() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgres://ecom_user:s3cret@dpg-abc123.oregon-postgres.render.com:5432/ecommerce_db");

        assertThat(props.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc123.oregon-postgres.render.com:5432/ecommerce_db");
        assertThat(props.get("spring.datasource.username")).isEqualTo("ecom_user");
        assertThat(props.get("spring.datasource.password")).isEqualTo("s3cret");
        assertThat(props.get("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
    }

    @Test
    void defaultsThePortWhenTheUrlOmitsIt() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgres://user:pass@db.internal/ecommerce");

        assertThat(props.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.internal:5432/ecommerce");
    }

    @Test
    void preservesQueryParametersSuchAsSslMode() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgres://user:pass@host:5432/db?sslmode=require");

        assertThat(props.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/db?sslmode=require");
    }

    @Test
    void translatesNeonUrlAndDropsLibpqOnlyParameters() {
        // Neon's connection strings carry channel_binding, which pgjdbc does not
        // understand — it must be stripped while sslmode is preserved.
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgresql://neondb_owner:placeholder@ep-plain-mountain-ayg8g110-pooler"
                        + ".c-5.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

        assertThat(props.get("spring.datasource.url")).isEqualTo(
                "jdbc:postgresql://ep-plain-mountain-ayg8g110-pooler.c-5.us-east-2.aws.neon.tech"
                        + ":5432/neondb?sslmode=require");
        assertThat(props.get("spring.datasource.url").toString()).doesNotContain("channel_binding");
        assertThat(props.get("spring.datasource.username")).isEqualTo("neondb_owner");
        assertThat(props.get("spring.datasource.password")).isEqualTo("placeholder");
    }

    @Test
    void dropsTheQueryStringEntirelyWhenOnlyLibpqParametersRemain() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgresql://u:p@host:5432/db?channel_binding=require");

        assertThat(props.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/db");
    }

    @Test
    void decodesPercentEncodedCredentials() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.translate(
                "postgres://user%40acme:p%40ss%3Aword@host:5432/db");

        assertThat(props.get("spring.datasource.username")).isEqualTo("user@acme");
        assertThat(props.get("spring.datasource.password")).isEqualTo("p@ss:word");
    }

    @Test
    void appliesToEnvironmentWhenOnlyDatabaseUrlIsPresent() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgres://u:p@host:5432/db");

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/db");
    }

    @Test
    void explicitDbUrlWinsOverPlatformDatabaseUrl() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:mysql://localhost:3306/ecommerce_db")
                .withProperty("DATABASE_URL", "postgres://u:p@host:5432/db");

        processor.postProcessEnvironment(env, new SpringApplication());

        // the operator's explicit JDBC URL must not be overwritten
        assertThat(env.getPropertySources().contains("platform-database-url")).isFalse();
    }

    @Test
    void ignoresAnUnsetOrNonPostgresDatabaseUrl() {
        MockEnvironment empty = new MockEnvironment();
        processor.postProcessEnvironment(empty, new SpringApplication());
        assertThat(empty.getPropertySources().contains("platform-database-url")).isFalse();

        MockEnvironment mysql = new MockEnvironment()
                .withProperty("DATABASE_URL", "mysql://user:pass@host:3306/db");
        processor.postProcessEnvironment(mysql, new SpringApplication());
        assertThat(mysql.getPropertySources().contains("platform-database-url")).isFalse();
    }
}
