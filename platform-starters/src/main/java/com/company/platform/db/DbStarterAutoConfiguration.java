package com.company.platform.db;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;

import java.util.Locale;

@AutoConfiguration
@EnableConfigurationProperties(DbProperties.class)
public class DbStarterAutoConfiguration {

    @Bean
    @ConditionalOnClass(HibernatePropertiesCustomizer.class)
    public HibernatePropertiesCustomizer platformHibernatePropertiesCustomizer(DbProperties properties) {
        return hibernateProperties -> {
            String dbType = normalize(properties.getType());
            if ("sybase".equals(dbType)) {
                hibernateProperties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.SybaseASEDialect");
                hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "30");
                hibernateProperties.put(AvailableSettings.ORDER_UPDATES, "true");
                hibernateProperties.put(AvailableSettings.ORDER_INSERTS, "true");
            } else {
                hibernateProperties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.OracleDialect");
                hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "50");
                hibernateProperties.put(AvailableSettings.BATCH_VERSIONED_DATA, "true");
                hibernateProperties.put(AvailableSettings.JDBC_TIME_ZONE, "UTC");
            }
            hibernateProperties.put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, ImplicitNamingStrategyJpaCompliantImpl.class.getName());
            hibernateProperties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName());
            hibernateProperties.putIfAbsent(AvailableSettings.FORMAT_SQL, "true");
            hibernateProperties.putIfAbsent(AvailableSettings.SHOW_SQL, "false");
        };
    }

    @Bean
    @ConditionalOnClass(HikariDataSource.class)
    public BeanPostProcessor platformHikariCustomizer(DbProperties properties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HikariDataSource hikariDataSource) {
                    DbProperties.Pool pool = properties.getPool();
                    hikariDataSource.setMaximumPoolSize(pool.getMaximumPoolSize());
                    hikariDataSource.setMinimumIdle(pool.getMinimumIdle());
                    hikariDataSource.setConnectionTimeout(pool.getConnectionTimeoutMs());
                    hikariDataSource.setIdleTimeout(pool.getIdleTimeoutMs());
                    hikariDataSource.setMaxLifetime(pool.getMaxLifetimeMs());
                    hikariDataSource.addDataSourceProperty("cachePrepStmts", "true");
                    hikariDataSource.addDataSourceProperty("prepStmtCacheSize", "250");
                    hikariDataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                }
                return bean;
            }
        };
    }

    private String normalize(String type) {
        return type == null ? "oracle" : type.trim().toLowerCase(Locale.ROOT);
    }
}
