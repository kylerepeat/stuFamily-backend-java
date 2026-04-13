package com.stufamily.backend.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration
public class MybatisCompatibilityConfig {

    @Bean
    public static BeanFactoryPostProcessor mybatisFactoryBeanObjectTypeFixer() {
        return new MybatisFactoryBeanObjectTypeFixer();
    }

    static final class MybatisFactoryBeanObjectTypeFixer implements BeanFactoryPostProcessor, PriorityOrdered {
        private static final Logger log = LoggerFactory.getLogger(MybatisFactoryBeanObjectTypeFixer.class);

        @Override
        public int getOrder() {
            return PriorityOrdered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            ClassLoader classLoader = beanFactory.getBeanClassLoader();
            String objectTypeAttr = FactoryBean.OBJECT_TYPE_ATTRIBUTE;
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                Object value = beanDefinition.getAttribute(objectTypeAttr);
                if (!(value instanceof String typeName) || !StringUtils.hasText(typeName)) {
                    continue;
                }
                try {
                    Class<?> resolvedType = ClassUtils.forName(typeName, classLoader);
                    beanDefinition.setAttribute(objectTypeAttr, resolvedType);
                } catch (Throwable ex) {
                    log.debug("Skip unresolved factoryBeanObjectType for bean: {}, type: {}", beanName, typeName, ex);
                }
            }
        }
    }
}
