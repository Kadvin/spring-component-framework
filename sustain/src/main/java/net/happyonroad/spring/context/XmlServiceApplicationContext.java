/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.ApplicationContext;

/**
 * 基于XML方式构建的 Service Application Context
 */
public class XmlServiceApplicationContext extends XmlComponentApplicationContext
        implements ServiceApplicationContext {

    public XmlServiceApplicationContext(Component component, ClassRealm resourceLoader, ApplicationContext parent) {
        super(component, resourceLoader, parent);
        this.setDisplayName("Service Context for: [" + component.getDisplayName() + "]");
    }

}
