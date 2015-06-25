/**
 * Developer: Kadvin Date: 14-6-10 上午9:35
 */
package net.happyonroad.spring.context;

import net.happyonroad.spring.support.SmartApplicationEventMulticaster;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.*;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.*;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

/**
 * 将各个context无法DRY的功能集中到这里
 */
public class ContextUtils {
    private final static Map<String, Set<ApplicationEvent>> events      = new HashMap<String, Set<ApplicationEvent>>();

    /**
     * 继承上级上下文中的属性配置器
     *
     * @param inheriting  上级上下文
     * @param context 当前上下文
     */
    public static void inheritParentProperties(ApplicationContext inheriting,
                                               GenericApplicationContext context) {
        if (!(inheriting instanceof AbstractApplicationContext)) return;
        List<BeanFactoryPostProcessor> processors =
                ((AbstractApplicationContext) inheriting).getBeanFactoryPostProcessors();
        for (BeanFactoryPostProcessor processor : processors) {
            if (processor instanceof PropertyResourceConfigurer)
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

    private static void setApplicationEventMulticaster(GenericApplicationContext context,
                                                         ApplicationEventMulticaster applicationEventMulticaster) {
        try {
            FieldUtils.writeField(context, "applicationEventMulticaster", applicationEventMulticaster, true);
        } catch (IllegalAccessException e) {
            throw new ApplicationContextException("Can't hacking the spring context for applicationEventMulticaster", e);
        }
    }

    public static void publishEvent(List<ApplicationContext> contexts, ApplicationEvent event){
        filterContexts(contexts, event);
        registerEvents(contexts, event);
        try {
            innerPublish(contexts, event);
        } finally {
            cleanEvents(contexts, event);
        }
    }

    private static void registerEvents(List<ApplicationContext> contexts, ApplicationEvent event) {
        for (ApplicationContext context : contexts) {
            Set<ApplicationEvent> set = events.get(context.getId());
            if( set == null ) {
                set = new HashSet<ApplicationEvent>();
                events.put(context.getId(), set);
            }
            set.add(event);
        }
    }

    private static void filterContexts(List<ApplicationContext> contexts, ApplicationEvent event) {
        Iterator<ApplicationContext> it = contexts.iterator();
        while (it.hasNext()) {
            ApplicationContext context = it.next();
            Set<ApplicationEvent> set = events.get(context.getId());
            if( set != null && set.contains(event)) it.remove();
        }
    }

    private static void cleanEvents(List<ApplicationContext> contexts, ApplicationEvent event) {
        for (ApplicationContext context : contexts) {
            Set<ApplicationEvent> set = events.get(context.getId());
            if( set != null) set.remove(event);
        }
    }

    protected static void innerPublish(List<ApplicationContext> contexts, ApplicationEvent event) {
        Logger eventLogger = getEventLogger(event);
        //向所有的context发布，context里面有防止重复的机制
        // 2. 提速
        Set<ApplicationListener<ApplicationEvent>> listeners = new HashSet<ApplicationListener<ApplicationEvent>>();
        for (ApplicationContext context : contexts) {
            if( context != null ) {
                if( context instanceof ConfigurableApplicationContext){
                    if( !((ConfigurableApplicationContext) context).isActive() ) continue;
                }
                ApplicationEventMulticaster multicaster = (ApplicationEventMulticaster) context.getBean(
                        AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
                Collection<ApplicationListener<?>> partListeners;
                if( multicaster instanceof SmartApplicationEventMulticaster){
                    SmartApplicationEventMulticaster smartOne = (SmartApplicationEventMulticaster) multicaster;
                    partListeners = smartOne.getApplicationListeners(event);
                } else {
                    try {
                        Class<AbstractApplicationEventMulticaster> klass = AbstractApplicationEventMulticaster.class;
                        Method method = klass.getDeclaredMethod("getApplicationListeners", ApplicationEvent.class);
                        method.setAccessible(true);
                        //noinspection unchecked
                        partListeners = (Collection<ApplicationListener<?>>) method.invoke(multicaster, event);
                    } catch (Exception e) {
                        eventLogger.warn("Can't steal event listener from " + multicaster);
                        partListeners = Collections.emptyList();
                    }
                }
                for (ApplicationListener<?> listener : partListeners) {
                    //noinspection unchecked
                    listeners.add((ApplicationListener<ApplicationEvent>) listener);
                }
            }
        }
        if( !listeners.isEmpty() ){
            LinkedList<ApplicationListener<ApplicationEvent>> list = new LinkedList<ApplicationListener<ApplicationEvent>>();
            //noinspection unchecked
            list.addAll(listeners);
            OrderComparator.sort(list);
            if( eventLogger.isInfoEnabled() ){
                StringBuilder sb = new StringBuilder();
                Iterator<ApplicationListener<ApplicationEvent>> it = list.iterator();
                while (it.hasNext()) {
                    ApplicationListener<ApplicationEvent> listener = it.next();
                    sb.append(listener.getClass().getSimpleName()).append("(");
                    if( listener instanceof Ordered){
                        sb.append(((Ordered) listener).getOrder());
                    }else{
                        sb.append("0");
                    }
                    sb.append(")");
                    if( it.hasNext() ) sb.append(",");
                }
                eventLogger.debug("Publish {} of {} to {}", event.getClass().getSimpleName(),
                                 event.getSource().getClass().getSimpleName(), sb);
            }
            for (ApplicationListener<ApplicationEvent> listener : list) {
                listener.onApplicationEvent(event);
            }
        }
    }

    private static Logger getEventLogger(ApplicationEvent event){
        return LoggerFactory.getLogger(event.getClass());
    }

}
