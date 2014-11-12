/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.container.support.ComponentClassLoader;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.Features;
import net.happyonroad.component.core.support.DefaultComponent;

/** 静态特性解析 */
public class StaticFeatureResolver extends AbstractFeatureResolver{

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
        ClassLoader realm = new ComponentClassLoader(component);
        if(component instanceof DefaultComponent){
            ((DefaultComponent)component).setClassLoader(realm);
        }
        resolveContext.registerFeature(component, getName(), realm);
    }
}
