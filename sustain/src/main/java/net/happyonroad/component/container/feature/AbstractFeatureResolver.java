/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentContext;
import net.happyonroad.component.core.FeatureResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 默认 */
public abstract class AbstractFeatureResolver implements FeatureResolver {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private   int              loadOrder;//加载优先级，越大越放在前面
    private   int              unloadOrder;//加载优先级，越大越放在前面
    protected ComponentContext resolveContext;

    public AbstractFeatureResolver(int loadOrder, int unloadOrder) {
        this.loadOrder = loadOrder;
        this.unloadOrder = unloadOrder;
    }

    @Override
    public FeatureResolver bind(ComponentContext context) {
        this.resolveContext = context;
        return this;
    }

    public int getLoadOrder() {
        return loadOrder;
    }

    public int getUnloadOrder() {
        return unloadOrder;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void beforeResolve(Component component) {
        // do nothing
    }

    @Override
    public void afterResolve(Component component) {
        // do nothing
    }

    @Override
    public Object release(Component component) {
        logger.debug("Release {} {} feature", component, getName());
        return resolveContext.removeFeature(component, getName());
    }
}
