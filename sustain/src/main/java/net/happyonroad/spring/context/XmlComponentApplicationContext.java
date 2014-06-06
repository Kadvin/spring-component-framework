/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import net.happyonroad.spring.SpringPathMatchingResourcePatternResolver;
import net.happyonroad.spring.support.SmartApplicationEventMulticaster;
import org.apache.commons.lang.reflect.FieldUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于XML的Spring Component Application Context
 */
public class XmlComponentApplicationContext extends GenericXmlApplicationContext implements
                                                                                 ComponentApplicationContext {
    private final Component component;

    public XmlComponentApplicationContext(Component component, ClassRealm realm, ApplicationContext parent) {
        this.setParent(parent); /*It accept null*/
        this.component = component;
        if (parent != null) {
            try {
                //把上级context的属性配置器加到当前这个上下文的post processor里面来
                //以便把其了解的属性配置到本context里面的bean的placeholder中
                Map<String, PropertyResourceConfigurer> beans = new HashMap<String, PropertyResourceConfigurer>();
                digPRC(parent, beans);
                for (PropertyResourceConfigurer configurer : beans.values()) {
                    this.addBeanFactoryPostProcessor(configurer);
                }
            } catch (BeansException e) {
                //skip it: e.printStackTrace();
            }
        }
        this.setDisplayName("Application Context for: [" + component.getDisplayName() + "]");
        this.setValidating(false);
        this.setClassLoader(realm);
    }

    private void digPRC(ApplicationContext context, Map<String, PropertyResourceConfigurer> container) {
        container.putAll(context.getBeansOfType(PropertyResourceConfigurer.class));
        if (context.getParent() != null) {
            digPRC(context.getParent(), container);
        }
    }

    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        return new SpringPathMatchingResourcePatternResolver(this);
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    protected void initApplicationEventMulticaster() {
        ApplicationEventMulticaster multicaster;
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            multicaster =
                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
            if (logger.isDebugEnabled()) {
                logger.debug("Using ApplicationEventMulticaster [" + multicaster + "]");
            }
        } else {
            multicaster = new SmartApplicationEventMulticaster(beanFactory);
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, multicaster);
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
                             APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
                             "': using default [" + multicaster + "]");
            }
        }
        setApplicationEventMulticaster(multicaster);
    }

    protected void setApplicationEventMulticaster(ApplicationEventMulticaster applicationEventMulticaster) {
        try {
            FieldUtils.writeField(this, "applicationEventMulticaster", applicationEventMulticaster, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't hacking the spring context for applicationEventMulticaster", e);
        }
    }
}
