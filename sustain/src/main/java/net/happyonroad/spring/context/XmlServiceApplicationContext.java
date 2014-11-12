/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * 基于XML方式构建的 Service Application Context
 *
 * Service Application Context 里面主要有两种bean
 * <ol><li> 服务定义->服务Import/Export的Bean, 这相当于FactoryBean
 * <li> 引入的服务bean（一般是proxy模式，原服务的内部特征接口并不会生效)
 * </ol>
 * <pre>这句话的意思是，如果某个 服务，原先声明了 ApplicationListener这种内部实现接口，
 * 但其作为服务被导入时，导入者只看到其Service接口，service context不会派发事件给它
 * 类似的内部接口还有很多...
 * </pre>
 */
public class XmlServiceApplicationContext extends XmlComponentApplicationContext
        implements ServiceApplicationContext {

    public XmlServiceApplicationContext(Component component, ClassLoader resourceLoader, AbstractApplicationContext parent) {
        super(component, resourceLoader, parent);
        this.setDisplayName("Service Context for: [" + component.getDisplayName() + "]");
    }

}
