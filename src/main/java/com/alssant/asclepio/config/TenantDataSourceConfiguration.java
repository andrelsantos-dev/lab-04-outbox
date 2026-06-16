package com.alssant.asclepio.config;

import com.alssant.asclepio.tenant.TenantAwareDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class TenantDataSourceConfiguration {
    @Bean
    public BeanPostProcessor tenantAwareDataSourcePostProcessor() {

        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {

                // 1. Prevents double wrapping if the process runs more than once
                if (bean instanceof TenantAwareDataSource) {
                    return bean;
                }

                // 2. Avoids applying the Tenant wrapper to Flyway's exclusive DataSource
                if ("flywayDataSource".equals(beanName)
                        || bean.getClass()
                        .getName()
                        .contains("Flyway")) {
                    return bean;
                }

                // 3. Applies the wrapper only to the main application DataSource
                if (bean instanceof DataSource ds) {
                    return new TenantAwareDataSource(ds);
                }

                return bean;
            }

        };

    }

}
