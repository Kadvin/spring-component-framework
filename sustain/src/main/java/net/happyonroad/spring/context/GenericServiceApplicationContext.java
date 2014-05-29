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
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 直接注入SpringServicePackage的Spring Service Application Context
 */
public class GenericServiceApplicationContext extends GenericApplicationContext
        implements  ServiceApplicationContext{

    private final Component component;
    public GenericServiceApplicationContext(Component component,
                                            ClassRealm realm,
                                            ApplicationContext parent) {
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
        this.setClassLoader(realm);
        this.setDisplayName("Service Context for: [" + component.getDisplayName() + "]");
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
