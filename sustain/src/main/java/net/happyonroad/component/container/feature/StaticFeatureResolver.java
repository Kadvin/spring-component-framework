/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.Features;

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
        ClassLoader realm = component.getClassLoader();
        resolveContext.registerFeature(component, getName(), realm);
    }
}
