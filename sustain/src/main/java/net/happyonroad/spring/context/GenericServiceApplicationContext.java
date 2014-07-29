/**
 * Developer: Kadvin Date: 14-5-28 下午1:05
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import net.happyonroad.spring.SpringPathMatchingResourcePatternResolver;
import org.apache.commons.lang.reflect.FieldUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 直接注入SpringServicePackage的Spring Service Application Context
 */
public class GenericServiceApplicationContext extends GenericApplicationContext
        implements  ServiceApplicationContext{

    private final Component component;
    public GenericServiceApplicationContext(Component component,
                                            ClassRealm realm,
                                            AbstractApplicationContext parent) {
        this.setParent(parent); /*It accept null*/
        this.component = component;
        ContextUtils.inheritParentProperties(parent, this);
        this.setClassLoader(realm);
        this.setDisplayName("Service Context for: [" + component.getDisplayName() + "]");
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
}
