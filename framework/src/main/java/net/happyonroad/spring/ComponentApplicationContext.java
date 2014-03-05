/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring;

import net.happyonroad.component.core.Component;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.Map;

/** Description */
public class ComponentApplicationContext extends GenericXmlApplicationContext {
    private final Component component;

    public ComponentApplicationContext(Component component, ClassRealm realm, ApplicationContext parent) {
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

    private void digPRC(ApplicationContext context, Map<String, PropertyResourceConfigurer> container){
        container.putAll(context.getBeansOfType(PropertyResourceConfigurer.class));
        if(context.getParent()!=null){
            digPRC(context.getParent(), container);
        }
    }

    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        return new SpringPathMatchingResourcePatternResolver(this);
    }

    public Component getComponent() {
        return component;
    }
}
