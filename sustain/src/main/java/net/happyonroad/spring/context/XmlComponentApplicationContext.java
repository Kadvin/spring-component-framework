/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import net.happyonroad.spring.SpringPathMatchingResourcePatternResolver;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 基于XML的Spring Component Application Context
 */
public class XmlComponentApplicationContext extends GenericXmlApplicationContext
        implements ComponentApplicationContext {
    private final Component component;

    public XmlComponentApplicationContext(Component component, ClassRealm realm, AbstractApplicationContext parent) {
        this.setParent(parent); /*It accept null*/
        this.component = component;
        ContextUtils.inheritParentProperties(parent, this);
        this.setDisplayName("Application Context for: [" + component.getDisplayName() + "]");
        this.setValidating(false);
        this.setClassLoader(realm);
        ContextUtils.applyComponentToResourcePatternResolver(this, component);
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
        ContextUtils.initApplicationEventMulticaster(this);
    }
}
