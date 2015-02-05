/**
 * Developer: Kadvin Date: 14-6-10 上午9:35
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import net.happyonroad.spring.SpringPathMatchingResourcePatternResolver;
import net.happyonroad.spring.support.SmartApplicationEventMulticaster;
import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.List;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

/**
 * 将各个context无法DRY的功能集中到这里
 */
public class ContextUtils {
    /**
     * 继承上级上下文中的属性配置器
     *
     * @param inheriting  上级上下文
     * @param context 当前上下文
     */
    public static void inheritParentProperties(ApplicationContext inheriting,
                                               GenericApplicationContext context) {
        if (!(inheriting instanceof AbstractApplicationContext)) return;
        List<BeanFactoryPostProcessor> processors = ((AbstractApplicationContext) inheriting).getBeanFactoryPostProcessors();
        for (BeanFactoryPostProcessor processor : processors) {
            if( processor instanceof PropertyResourceConfigurer)
                context.addBeanFactoryPostProcessor(processor);
        }
    }

    /**
     * 修改特定上下文的Event Multicaster的默认实现
     *
     * @param context 被修改的上下文
     */
    public static void initApplicationEventMulticaster(GenericApplicationContext context) {
        ApplicationEventMulticaster multicaster;
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            multicaster =
                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        } else {
            multicaster = new SmartApplicationEventMulticaster(beanFactory);
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, multicaster);
        }
        setApplicationEventMulticaster(context, multicaster);

    }

    public static void applyComponentToResourcePatternResolver(GenericApplicationContext context, Component component){
        try {
            SpringPathMatchingResourcePatternResolver rpr =
                    (SpringPathMatchingResourcePatternResolver) FieldUtils.readField(context, "resourcePatternResolver", true);
            rpr.setComponent(component);
        } catch (IllegalAccessException e) {
            throw new ApplicationContextException("Can't hacking the spring context for resourcePatternResolver", e);
        }

    }

    private static void setApplicationEventMulticaster(GenericApplicationContext context,
                                                         ApplicationEventMulticaster applicationEventMulticaster) {
        try {
            FieldUtils.writeField(context, "applicationEventMulticaster", applicationEventMulticaster, true);
        } catch (IllegalAccessException e) {
            throw new ApplicationContextException("Can't hacking the spring context for applicationEventMulticaster", e);
        }
    }

}
