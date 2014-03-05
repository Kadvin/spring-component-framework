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
    private   int              priority;//优先级，越大越放在前面
    protected ComponentContext resolveContext;

    public AbstractFeatureResolver(int priority) {
        this.priority = priority;
    }

    @Override
    public FeatureResolver bind(ComponentContext context) {
        this.resolveContext = context;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(FeatureResolver another) {
        return another.getPriority() - priority;
    }

    @Override
    public String toString() {
        return getName();
    }


    @Override
    public Object release(Component component) {
        logger.info("Release {} {} feature", getName(), component);
        return resolveContext.removeFeature(component, getName());
    }
}
