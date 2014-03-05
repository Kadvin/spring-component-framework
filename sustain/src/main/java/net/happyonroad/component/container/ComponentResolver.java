/**
 * @author XiongJie, Date: 13-9-13
 */
package net.happyonroad.component.container;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.Dependency;

import java.io.File;
import java.io.InputStream;

/**
 * 组件解析器
 */
public interface ComponentResolver {
    /**
     * 从某个 pom.xml 文件中解析出组件对象
     * 该函数内部将会将组件的其他关联对象全部解析出来，包括:
     * <ol><li> parent   ：解析
     * <li> dependencies ：会触发相应组件的解析，但不会建立关联关系
     * <li> modules      ：解析
     * </ol>
     * TODO 考虑是不是增加一个可控的接口，形如: resolveComponent(stream, flags)
     * 当前仅作了compile依赖，不做test,system等其他scope
     * @param dependency 引入该组件的依赖信息，主要是其中可能带有部分排除信息
     * @param pomDotXml 组件的pom.xml文件流
     * @return 组件实例
     * @throws InvalidComponentException 当文件名称与jar中的pom.xml文件信息不吻合时，抛出该异常
     * @throws net.happyonroad.component.core.exception.InvalidComponentNameException 当组件名称错误，抛出该异常
     * @throws net.happyonroad.component.core.exception.DependencyNotMeetException 当相关的其他依赖没有满足时，抛出该异常
     */
    Component resolveComponent(Dependency dependency, InputStream pomDotXml) throws InvalidComponentException, DependencyNotMeetException, InvalidComponentNameException;

    /**
     * 为某个依赖，从文件中解析出特定的组件信息
     * @param dependency 引入该组件的依赖信息，主要是其中可能带有部分排除信息
     * @param jarOrPomFilePath jar 或者 pom文件的路径，该文件名称必须有正确的格式化，具体来讲，就是应该遵循：
     *                         groupId.artifactId-version.type
     * @return 解析出来的组件实例
     * @throws InvalidComponentException 当文件名称与jar中的pom.xml文件信息不吻合时，抛出该异常
     * @throws InvalidComponentNameException 当组件名称错误，抛出该异常
     * @throws DependencyNotMeetException 当相关的其他依赖没有满足时，抛出该异常
     */
    Component resolveComponent(Dependency dependency, File jarOrPomFilePath) throws InvalidComponentException, InvalidComponentNameException, DependencyNotMeetException;
}
