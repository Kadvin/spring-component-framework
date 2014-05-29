/**
 * Developer: Kadvin Date: 14-5-28 下午1:05
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import net.happyonroad.spring.SpringPathMatchingResourcePatternResolver;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于Annotation Config的Spring Component Application Context
 */
public class AnnotationComponentApplicationContext extends AnnotationConfigApplicationContext
        implements ComponentApplicationContext {

    private final Component component;

    public AnnotationComponentApplicationContext(Component component, ClassRealm realm, ApplicationContext parent) {
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

    @Override
    public Component getComponent() {
        return component;
    }
}
