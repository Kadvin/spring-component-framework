/**
 * @author XiongJie, Date: 13-11-11
 */

package net.happyonroad.component.core;

import net.happyonroad.component.container.*;
import net.happyonroad.component.container.support.DefaultServiceHelper;
import org.springframework.context.ApplicationContext;

import java.util.List;

/** 组件上下文 */
public interface ComponentContext {
    /**
     * 为特定组件注册特定名称的特性结果
     *
     * @param component 特定组件
     * @param name      特性名称
     * @param feature   特性结果
     */
    void registerFeature(Component component, String name, Object feature);

    Object removeFeature(Component component, String name);

    ClassLoader getClassRealm(String componentId);

    <T> T getFeature(Component component, String name);

    ClassLoader getLibraryFeature(Component component);

    ApplicationContext getApplicationFeature(Component component);

    //按照对应组件的依赖顺序排序的应用程序上下文
    List<ApplicationContext> getApplicationFeatures();

    ServiceRegistry getRegistry();

    DefaultServiceHelper getServiceHelper();

    ComponentLoader getComponentLoader();

    ComponentRepository getComponentRepository();

    ApplicationContext getContext();

}
