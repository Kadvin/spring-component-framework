/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.core;

import java.io.IOException;

/** 组件的特性解析器 */
public interface FeatureResolver {

    Features AggregatingFlag = new Features("Aggregating", new Object());
    Features PlainFlag = new Features("Plain", new Object());

    String getName();

    /**
     * 绑定解析的上下文
     * @param context 上下文
     */
    FeatureResolver bind(ComponentContext context);

    /**
     * 加载顺序，越小越靠前
     * @return priority
     */
    int getLoadOrder();

    /**
     * 卸载优先级，越小越靠前
     * @return priority
     */
    int getUnloadOrder();

    /**
     * 判断组件是否有本特性
     *
     * @param component 被判断的组件
     * @return 是否有特性
     */
    boolean hasFeature(Component component);

    /**
     * 在特定的上下文中解析相应的组件
     *
     * @param component 被解析的组件
     *
     */
    void resolve(Component component) throws IOException;

    /**
     * 在特定的上下文中卸载/释放相应的组件
     * @param component 被卸载的组件
     */
    Object release(Component component) ;
}
