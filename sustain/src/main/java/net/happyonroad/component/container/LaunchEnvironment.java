package net.happyonroad.component.container;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;

import java.io.IOException;

/**
 * 启动环境
 */
public interface LaunchEnvironment {
    AppLauncher createLauncher(Component component) throws IOException;

    void execute(AppLauncher launcher, String[] args) throws Exception;

    Component resolveComponent(String strDependency) throws DependencyNotMeetException, InvalidComponentNameException;

    void shutdown();

    /**
     * 整体加载某个组件，如果存在父组件，则先加载父组件，如果存在对其他组件的依赖，则先加载被依赖的组件
     *
     * @param component 被加载的组件
     */
    void load(Component component) throws Exception;

    /**
     * 整体卸载某个组件，如果存在父组件或者依赖组件，则会在卸载自身之后，先卸载依赖组件，再卸载父组件
     *
     * @param component 被卸载的组件
     */
    void unload(Component component);
}
