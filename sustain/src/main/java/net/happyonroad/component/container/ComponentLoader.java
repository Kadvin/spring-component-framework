package net.happyonroad.component.container;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.FeatureResolver;

/** 组件加载接口 */
public interface ComponentLoader {
    /**
     * 判断组件是否已经加载
     *
     * @param component 被加载的组件
     * @return 判断结果
     */
    boolean isLoaded(Component component);

    /**
     * 实际加载某个组件，如果组件存在依赖，会自动预先加载相关依赖
     *
     * @param component 被加载的组件
     */
    void load(Component component) throws Exception;

    /**
     * 实际卸载某个已经加载的组件，如果组件存在依赖，会自动卸载其依赖
     *
     * @param component 被卸载的组件
     */
    void unload(Component component);

    /**
     * 仅卸载组件自身，不进行关联和依赖卸载
     * @param component 被卸载的组件
     */
    void unloadSingle(Component component);

    /**
     * 注册特性解析器
     *
     * @param resolver 解析器
     */
    void registerResolver(FeatureResolver resolver);

    /**
     * 获取某个特性的解析器
     *
     * @param name 特性名称
     * @return 解析器，找不到则返回null
     */
    <T extends FeatureResolver> T getFeatureResolver(String name);

}
