/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.Features;
import net.happyonroad.spring.support.CombinedMessageSource;
import net.happyonroad.spring.support.ObservableMessageSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.springframework.context.ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;

/** 静态特性解析 */
public class StaticFeatureResolver extends AbstractFeatureResolver{
    public static final String APP_MESSAGE = "App-Message";

    public StaticFeatureResolver() {
        super(10, 100);
    }

    @Override
    public String getName() {
        return Features.STATIC_FEATURE;
    }

    @Override
    public boolean hasFeature(Component component) {
        return component.isApplication() && component.getResource().isPomXmlExists();
    }

    @Override
    public void resolve(Component component) {
        if( component.isPlain()) return;
        logger.debug("Resolving {} {} feature", component, getName());
        ApplicationContext parent = resolveContext.getRootContext() ;
        ClassLoader realm = component.getClassLoader();
        //在根据配置的情况下，根据 manifest里面的App-Message加载资源
        String appMessage = getAppMessage(component);
        if(StringUtils.isNotBlank(appMessage)){
            CombinedMessageSource combined  = parent.getBean(CombinedMessageSource.class);
            ObservableMessageSource source = new ObservableMessageSource();
            source.setBundleClassLoader(realm);
            source.setBasenames(StringUtils.split(appMessage, CONFIG_LOCATION_DELIMITERS));
            combined.combine(source);
        }
        resolveContext.registerFeature(component, getName(), realm);
    }

    @Override
    public Object release(Component component) {
        ApplicationContext parent = resolveContext.getRootContext() ;
        String appMessage = getAppMessage(component);
        if(StringUtils.isNotBlank(appMessage)){
            ClassLoader realm = component.getClassLoader();
            CombinedMessageSource combined  = parent.getBean(CombinedMessageSource.class);
            String[] names = StringUtils.split(appMessage, CONFIG_LOCATION_DELIMITERS);
            combined.unbind(realm, names);
        }
        return super.release(component);
    }

    protected String getAppMessage(Component component){
        return component.getManifestAttribute(APP_MESSAGE);
    }
}
