/**
 * Developer: Kadvin Date: 14-5-28 下午1:05
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 基于Annotation Config的Spring Component Application Context
 */
public class AnnotationComponentApplicationContext extends AnnotationConfigApplicationContext
        implements ComponentApplicationContext {

    private final Component component;

    public AnnotationComponentApplicationContext(Component component,
                                                 ApplicationContext parent) {
        this.setParent(parent); /*It accept null*/
        this.component = component;
        ContextUtils.inheritParentProperties(parent, this);
        this.setDisplayName("Application Context for: [" + component.getDisplayName() + "]");
        this.setClassLoader(component.getClassLoader());
        this.setResourceLoader(component.getResourceLoader());
        //必须/只能在构造时，将component与SpringPathMatchingResourcePatternResolver绑定在一起
        // getResourcePatternResolver()由父类调用，此时component尚未被赋值
        //ContextUtils.applyComponentToResourcePatternResolver(this, component);
    }


    @Override
    public Component getComponent() {
        return component;
    }


    @Override
    protected void initApplicationEventMulticaster() {
        ContextUtils.initApplicationEventMulticaster(this);
    }

}
