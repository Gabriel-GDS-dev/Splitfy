package com.example.Splitfy.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import com.example.Splitfy.SplitfyApplication;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.main.lazy-initialization", havingValue = "true")
public class ConfiguracaoJpaLazy {

    @Bean
    @Primary
    @DependsOnDatabaseInitialization
    FabricaJpaLazy entityManagerFactory(EntityManagerFactoryBuilder builder, DataSource dataSource,
                                       JpaProperties properties,
                                       ResourceLoader resourceLoader) {
        var delegate = builder.dataSource(dataSource).packages(SplitfyApplication.class)
                .mappingResources(properties.getMappingResources().toArray(String[]::new)).build();
        delegate.setResourceLoader(resourceLoader);
        delegate.setBeanClassLoader(resourceLoader.getClassLoader());
        return new FabricaJpaLazy(delegate);
    }

    // The default factory implements LoadTimeWeaverAware, which Spring initializes
    // before starting HTTP even when it is lazy. This wrapper defers it until use.
    static class FabricaJpaLazy extends AbstractFactoryBean<EntityManagerFactory>
            implements PersistenceExceptionTranslator {
        private final LocalContainerEntityManagerFactoryBean delegate;

        FabricaJpaLazy(LocalContainerEntityManagerFactoryBean delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<?> getObjectType() {
            return EntityManagerFactory.class;
        }

        @Override
        protected EntityManagerFactory createInstance() {
            delegate.afterPropertiesSet();
            delegate.afterSingletonsInstantiated();
            return delegate.getObject();
        }

        @Override
        protected void destroyInstance(EntityManagerFactory instance) {
            delegate.destroy();
        }

        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            return delegate.translateExceptionIfPossible(ex);
        }
    }
}
