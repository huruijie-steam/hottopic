package com.yupi.hottopic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 显式 DataSource(解决 Boot 4 下 MyBatis-Plus 的
 * @ConditionalOnSingleCandidate(DataSource) 自动配置排序评估失败问题)。
 * 属性来自 spring.datasource.*。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
