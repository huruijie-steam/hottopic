package com.yupi.hottopic.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 显式 DataSource(解决 Boot 4 下 MyBatis-Plus 的
 * @ConditionalOnSingleCandidate(DataSource) 自动配置排序评估失败问题)。
 * DataSourceProperties 由 DataSourceAutoConfiguration 提供,这里完成 url/username/password 到连接池的绑定。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
