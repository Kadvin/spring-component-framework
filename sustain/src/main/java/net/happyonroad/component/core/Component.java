package net.happyonroad.component.core;

import net.happyonroad.component.container.RepositoryScanner;
import net.happyonroad.component.container.support.ComponentClassLoader;
import net.happyonroad.component.core.support.Dependency;
import net.happyonroad.component.core.support.DependencyManagement;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/** 组件对外接口 */
@SuppressWarnings("unused")
public interface Component extends Comparable<Component>, Versionize {
    final String LATEST_VERSION   = "LATEST";
    final String SNAPSHOT_VERSION = "SNAPSHOT";
    final String RELEASE_VERSION  = "RELEASE";

    final Pattern VERSION_FILE_PATTERN = Pattern.compile("^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$");
    final Pattern FILE_NAME_PATTERN    =
            Pattern.compile("(^[-]+)-((?:\\d+)\\.(?:\\d+)\\.(?:\\d+))(?:\\.([^.]*))\\.(jar|pom)");

    final String SCOPE_COMPILE  = "compile";
    final String SCOPE_TEST     = "test";
    final String SCOPE_RUNTIME  = "runtime";
    final String SCOPE_PROVIDED = "provided";
    final String SCOPE_SYSTEM   = "system";
    final String SCOPE_IMPORT   = "import";   // Used to import dependencyManagement dependencies

    /**
     * <h2>组件标识和版本的结合</h2>
     * <p/>
     * 某种可视化/可标记化的表达方式
     * 大致结构为: groupId.artifactId-version-classifier.type
     *
     * @return 标识
     */
    String getId();

    /**
     * <h2>父组件</h2>
     * <p/>
     * <ul><li>在没有组件仓库支持时，父组件仅仅是一个基础的信息；</li>
     * <li>有仓库支持时，父组件是一个在仓库中挂了号的对象 </li> </ul>
     *
     * @return 父组件
     */
    Component getParent();

    /**
     * pom中定义的组件名称，可视化，可理解
     *
     * @return 组件名称
     */
    String getName();


    /**
     * 组件的显示名称
     *
     * @return 如果有name，则返回name，否则返回id
     */
    String getDisplayName();

    /**
     * pom中定义的组件描述，可视化，可理解
     *
     * @return 组件描述
     */
    String getDescription();

    /**
     * pom中定义的组件地址
     *
     * @return 组件地址
     */
    String getUrl();

    /**
     * 解析出该组件对应的文件，可能是pom文件，也可能是jar文件
     *
     * @return 组件文件
     */
    Resource getUnderlyingResource();


    /**
     * 得到该组件的url，形如: component:org.apache.maven-1.2.3.jar
     *
     * @return 该组件的url
     */
    URL getURL();

    /**
     * 用于读取组件内部文件流的资源封装
     *
     * @return 组件资源接口对象
     */
    ComponentResource getResource();

    /**
     * 获得相应的类加载器
     * @return 类加载器
     */
    ComponentClassLoader getClassLoader();

    /**
     * 组件依赖关系
     *
     * @return 依赖信息
     */
    List<Dependency> getDependencies();

    /**
     * 依赖管理器
     *
     * @return Maven中的依赖管理器，主要辅助解析
     */
    DependencyManagement getDependencyManagement();

    /**
     * 根据某个依赖信息，获取本组件内定义的依赖信息
     *
     * @param dependency 相关依赖
     * @return 已经定义的依赖
     */
    Dependency getDependency(Dependency dependency);

    /**
     * 获取包括父组件的依赖在内的所有依赖
     *
     * @return 依赖列表
     */
    Set<Dependency> getAllDependencies();

    /**
     * 获取包括父组件的依赖组件在内的所有依赖组件
     *
     * @return 依赖组件集合
     */
    Set<Component> getAllDependedComponents();

    /**
     * 组件的子模块
     *
     * @return 子模块
     */
    List<Component> getModules();

    /**
     * 取得所有依赖的组件对象
     * <p/>
     * 通过这个属性，支持在仓库中的组件之间形成了一张网状结构
     *
     * @return 解析完依赖信息之后的得到的相应组件
     */
    List<Component> getDependedComponents();

    /**
     * 判断组件是不是快照式组件
     * <p/>
     * 判断方式就是 classifier == 'SNAPSHOT'
     *
     * @return 快照否
     */
    boolean isSnapshot();

    /**
     * 判断组件是不是最终正式发布的组件
     *
     * @return 是否正式发布
     */
    boolean isRelease();

    /**
     * 仅仅是聚合项目，不是真实的Jar基础项目
     * (实则type = 'pom')
     *
     * @return 是否是聚合项目
     */
    boolean isAggregating();

    /**
     * 判断是否是当前项目的应用程序，而非第三方包
     * @return 非第三方包
     */
    boolean isApplication();

    /**
     * 获取jar文件的Manifest属性
     *
     * @param attributeName 属性名称
     * @return 属性值
     */
    String getManifestAttribute(String attributeName);

    /**
     * Maven为组件配置的额外属性
     *
     * @return 属性值
     */
    Properties getProperties();

    /**
     * 获取组件上下文中定义的变量
     *
     * @param key 变量的key，支持系统属性，如 project.*, pom.*
     * @return 相应的属性
     */
    String resolveVariable(String key);

    /**
     * 是否是简单的，普通的第三方包，没有按照我们的规范进行打包
     * @return 是否
     */
    boolean isPlain();

    /**
     * 判断一个组件是不是依赖于另外一个组件
     *
     * @param another 另外一个组件
     * @return 是否依赖
     */
    boolean dependsOn(Component another);

    /**
     * GroupId + ArtifactId + Version
     * @return brief id
     */
    String getBriefId();

    /**
     * 如果是应用组件，返回相应的Application上下文，如果不是，返回null
     *
     * @return Application Context
     */
    ApplicationContext getApplication();

    /**
     * 为组件设置额外的缺省manifest属性
     *
     * 这些属性会被实际配置的manifest属性覆盖
     *                 主要用于配置缺省值，减少配置工作量
     * @param key 额外的缺省属性key
     * @param value 额外的缺省属性值
     */
    void setManifestAttribute(String key, String value);

    Set<URL> getLibURLs();

    List<String> getAppBriefIds();

    void registerScanner(RepositoryScanner scanner);

    List<RepositoryScanner> getScanners();
}
