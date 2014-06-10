/**
 * @author XiongJie, Date: 13-10-31
 */
package net.happyonroad.spring.context;

import net.happyonroad.spring.support.CombinedMessageSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将多个Context组合成为一个 */
public class CombinedApplicationContext extends GenericApplicationContext {

    public CombinedApplicationContext(Set<AbstractApplicationContext> dependedContexts,
                                      List<ResourceBundleMessageSource> sources) {
        //不应该将依赖的bean的context里面的放过来
        //为了让系统能够正确的加载或者解析，先仅仅将PropertyConfigurer加过来
        //或者，property configurer也应该以服务的形式export再import?
        for (AbstractApplicationContext context : dependedContexts) {
            ContextUtils.inheritParentProperties(context, this);
            Map<String,ApplicationListener> parentListeners = context.getBeansOfType(ApplicationListener.class, true, false);
            //将被依赖的上下文中的listener放到这个组合上下文中，以便子上下文发出事件时，它们都能听到
            for (ApplicationListener listener : parentListeners.values()) {
                this.addApplicationListener(listener);
            }
        }

        StringBuilder name = new StringBuilder("Combined Context of: [");
        for (ApplicationContext context : dependedContexts) {
            name.append(componentName(context)).append(",");
        }
        name.deleteCharAt(name.length()-1);
        name.append("]");
        setDisplayName(name.toString());

        if( !sources.isEmpty() )
            this.getBeanFactory().registerSingleton("messageSource", new CombinedMessageSource(sources));
    }

    protected String componentName(ApplicationContext context) {
        return StringUtils.substringBetween(context.getDisplayName(), "[", "]");
    }

    @Override
    protected void initApplicationEventMulticaster() {
        ContextUtils.initApplicationEventMulticaster(this);
    }


}
