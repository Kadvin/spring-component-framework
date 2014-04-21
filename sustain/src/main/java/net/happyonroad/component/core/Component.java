package net.happyonroad.component.core;

import net.happyonroad.component.core.support.Dependency;
import net.happyonroad.component.core.support.DependencyManagement;

import java.io.File;
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
    File getFile();

    /**
     * 获取到该组件对应的Jar的url
     * <ul>
     * <li>如果组件是jar形态，则直接返回对应文件的url
     * <li>如果是pom的形态，且上级目录存在对应的jar，则返回上级目录的jar
     * <li>如果上级目录不存在对应的jar，则返回null
     * </ul>
     *
     * @return jar的url
     */
    URL getFileURL();

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
    ClassLoader getClassLoader();

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
     * 获得该组件所依赖的第三方包的url列表（所有）
     *
     * @return 第三方包url列表，如果没有依赖，也应该返回一个空数组
     */
    Set<URL> getDependedPlainURLs();

    /**
     * 判断一个组件是不是依赖于另外一个组件
     * @param another 另外一个组件
     * @return 是否依赖
     */
    boolean dependsOn(Component another);
}
