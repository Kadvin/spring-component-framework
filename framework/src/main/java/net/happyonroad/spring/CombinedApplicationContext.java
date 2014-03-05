/**
 * @author XiongJie, Date: 13-10-31
 */
package net.happyonroad.spring;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Set;

/** 将多个Context组合成为一个 */
public class CombinedApplicationContext extends GenericApplicationContext {

    public CombinedApplicationContext(Set<ApplicationContext> dependedContexts) {
        StringBuilder name = new StringBuilder("Combined Context of: [");
        //不应该将依赖的bean的context里面的放过来
        //为了让系统能够正确的加载或者解析，先仅仅将PropertyConfigurer加过来
        //或者，property configurer也应该以服务的形式export再import?
        for (ApplicationContext context : dependedContexts) {
            String[] beanNames = context.getBeanNamesForType(PropertyPlaceholderConfigurer.class);
            for (String beanName : beanNames) {
                Object bean = context.getBean(beanName);
                //在不同的context里面，可能有重复，忽略
                try {
                    this.getBeanFactory().registerSingleton(beanName, bean);
                } catch (Exception e) {
                    //skip it
                }
            }
            name.append(context.getDisplayName()).append(",");
        }
        name.deleteCharAt(name.length()-1);
        name.append("]");
        setDisplayName(name.toString());
    }

}
