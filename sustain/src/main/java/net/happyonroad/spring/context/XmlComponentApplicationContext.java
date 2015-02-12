/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * 基于XML的Spring Component Application Context
 */
public class XmlComponentApplicationContext extends GenericXmlApplicationContext
        implements ComponentApplicationContext {
    private final Component component;

    public XmlComponentApplicationContext(Component component, ApplicationContext parent) {
        this.setParent(parent); /*It accept null*/
        this.component = component;
        ContextUtils.inheritParentProperties(parent, this);
        this.setDisplayName("Application Context for: [" + component.getDisplayName() + "]");
        this.setValidating(false);
        this.setClassLoader(component.getClassLoader());
        this.setResourceLoader(component.getResourceLoader());
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
