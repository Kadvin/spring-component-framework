/**
 * Developer: Kadvin Date: 14-6-10 上午9:35
 */
package net.happyonroad.spring.context;

import net.happyonroad.spring.support.SmartApplicationEventMulticaster;
import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

/**
 * 将各个context无法DRY的功能集中到这里
 */
class ContextUtils {
    /**
     * 继承上级上下文中的属性配置器
     *
     * @param inheriting  上级上下文
     * @param context 当前上下文
     */
    public static void inheritParentProperties(AbstractApplicationContext inheriting,
                                               GenericApplicationContext context) {
        if (inheriting == null) return;
        Set<PropertyResourceConfigurer> configurers = new LinkedHashSet<PropertyResourceConfigurer>();
        configurers.addAll(inheriting.getBeansOfType(PropertyResourceConfigurer.class).values());
        //把上级context的属性配置器加到当前这个上下文的post processor里面来
        //以便把其了解的属性配置到本context里面的bean的placeholder中
        List<BeanFactoryPostProcessor> postProcessors = inheriting.getBeanFactoryPostProcessors();

        for (BeanFactoryPostProcessor postProcessor : postProcessors) {
            //现在暂时仅仅将 Property Resource Configurer 复制过来
            //  以后根据需求，可能还有其他类型的 post processor需要复制
            if( postProcessor instanceof PropertyResourceConfigurer){
                configurers.add((PropertyResourceConfigurer) postProcessor);
            }
        }
        for (PropertyResourceConfigurer configurer : configurers) {
            context.addBeanFactoryPostProcessor(configurer);
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

    private static void setApplicationEventMulticaster(GenericApplicationContext context,
                                                         ApplicationEventMulticaster applicationEventMulticaster) {
        try {
            FieldUtils.writeField(context, "applicationEventMulticaster", applicationEventMulticaster, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't hacking the spring context for applicationEventMulticaster", e);
        }
    }
}
