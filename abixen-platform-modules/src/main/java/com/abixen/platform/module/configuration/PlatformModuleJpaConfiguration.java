/**
 * Copyright (c) 2010-present Abixen Systems. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.abixen.platform.module.configuration;

import com.abixen.platform.module.security.PlatformAuditorAware;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;


@Configuration
@Import({PlatformModuleDataSourceConfiguration.class, PlatformModulePropertiesConfiguration.class})
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "platformAuditorAware")
@EnableJpaRepositories(basePackages = {"com.abixen.platform.module.chart.repository", "com.abixen.platform.module.magicnumber.repository", "com.abixen.platform.module.kpichart.repository"})
public class PlatformModuleJpaConfiguration {

    static Logger log = Logger.getLogger(PlatformModuleJpaConfiguration.class.getName());

    @Autowired
    DataSource dataSource;

    @Autowired
    Environment environment;

    @Value("#{jdbcProperties['jdbc.dialect']}")
    private String hibernateDialect;


    //http://java.dzone.com/articles/springmvc4-spring-data-jpa

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
        log.debug("entityManagerFactoryBean()");
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan(new String[]{"com.abixen.platform.module.model", "com.abixen.platform.module.chart.model", "com.abixen.platform.module.magicnumber.model", "com.abixen.platform.module.kpichart.model"});
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        Properties jpaProperties = new Properties();

        jpaProperties.put("hibernate.show_sql", "true");
        jpaProperties.put("hibernate.dialect", hibernateDialect);

        String activeProfile = environment.getActiveProfiles()[0];
        String createDbSchema = environment.getProperty("createDbSchema");

        if ((createDbSchema != null && createDbSchema.equalsIgnoreCase("true")) || activeProfile.equals("test")) {
            log.info("Import database will be executing. Active profile is " + activeProfile);
            jpaProperties.put("hibernate.hbm2ddl.auto", "create");
        } else {
            log.info("Import database won't be executing. Active profile is " + activeProfile);
            jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        }

        entityManagerFactoryBean.setJpaProperties(jpaProperties);
        entityManagerFactoryBean.afterPropertiesSet();
        entityManagerFactoryBean.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
        return entityManagerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean(name = "platformAuditorAware")
    public AuditorAware platformAuditorAware() {
        return new PlatformAuditorAware();
    }

}
