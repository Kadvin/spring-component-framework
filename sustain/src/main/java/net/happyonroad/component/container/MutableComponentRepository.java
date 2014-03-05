package net.happyonroad.component.container;

import net.happyonroad.component.core.Component;

/** 可修改的组件仓库 */
public interface MutableComponentRepository extends ComponentRepository {
    /**
     * 向仓库中注册一个组件
     *
     * @param component 新解析的组件
     */
    void addComponent(Component component);

    /**
     * 从仓库中移除一个错误组件
     *
     * @param component 解析有误的组件
     */
    void removeComponent(Component component);

    /**
     * 判断某个group是不是应用组
     *
     * @param groupId 组的id
     * @return 判断结果，所谓应用组，就是隶属于当前系统开发的应用，而不是第三方包
     */
    boolean isApplication(String groupId);

    /**
     * Repository的home目录
     *
     * @return home目录路径
     */
    String getHome();
}
