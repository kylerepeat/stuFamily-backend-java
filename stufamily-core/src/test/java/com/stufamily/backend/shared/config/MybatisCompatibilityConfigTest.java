package com.stufamily.backend.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

class MybatisCompatibilityConfigTest {

    @Test
    void shouldConvertFactoryBeanObjectTypeFromStringToClass() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(Object.class);
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, "java.lang.String");
        beanFactory.registerBeanDefinition("familyGroupMapper", beanDefinition);

        new MybatisCompatibilityConfig.MybatisFactoryBeanObjectTypeFixer().postProcessBeanFactory(beanFactory);

        Object fixed = beanFactory.getBeanDefinition("familyGroupMapper")
            .getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
        assertInstanceOf(Class.class, fixed);
        assertEquals(String.class, fixed);
    }
}
