package net.happyonroad.component.container;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.Dependency;

import java.io.File;
import java.util.List;

/**
 * 组件仓库
 */
public interface ComponentRepository{

    /**
     * 根据依赖找到或者解析出当前优先级最高的组件对象
     *
     * @param dependency 依赖信息
     * @return 组件对象
     */
    Component resolveComponent(Dependency dependency) throws InvalidComponentNameException, DependencyNotMeetException;

    /**
     * 直接根据某个依赖的字符串形式解析组件
     *
     * @param strDependency 形如： groupId.artifact-version-classifier格式
     * @return 组件对象
     * @throws DependencyNotMeetException
     * @throws InvalidComponentNameException
     */
    Component resolveComponent(String strDependency) throws DependencyNotMeetException, InvalidComponentNameException;

    /**
     * 根据依赖，解析出所有的候选组件
     *
     * @param dependency 依赖信息
     * @return 候选组件列表
     */
    List<Component> resolveComponents(Dependency dependency);


    /**
     * 根据候选的依赖情况，对文件进行排序，以便进行加载
     *
     * @param candidateComponentJars 候选组件的文件
     */
    void sortCandidates(File[] candidateComponentJars);
}
