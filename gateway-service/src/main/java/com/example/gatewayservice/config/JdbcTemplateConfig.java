package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean(name = "user")
    @ConfigurationProperties(prefix = "spring.datasource-user")
    public DataSource dataSource1() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "user-jdbc-template")
    public JdbcTemplate jdbcTemplate1(@Qualifier("user") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
