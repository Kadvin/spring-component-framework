/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring;

import net.happyonroad.component.core.Component;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.ApplicationContext;

/** Description */
public class ServiceApplicationContext extends ComponentApplicationContext {
    public ServiceApplicationContext(Component component, ClassRealm resourceLoader, ApplicationContext parent) {
        super(component, resourceLoader, parent);
        this.setDisplayName("Service Context for: [" + component.getDisplayName() + "]");
    }
}
