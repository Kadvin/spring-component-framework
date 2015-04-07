/**
 * @author XiongJie, Date: 13-9-13
 */
package net.happyonroad.component.container.support;

import com.thoughtworks.xstream.XStream;
import net.happyonroad.component.classworld.ManipulateClassLoader;
import net.happyonroad.component.container.ComponentResolver;
import net.happyonroad.component.container.MutableComponentRepository;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.*;
import net.happyonroad.spring.exception.ApplicationConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static net.happyonroad.component.core.support.ComponentUtils.relativePath;

/** 解析pom xml的缺省实现对象 */
public class DefaultComponentResolver implements ComponentResolver {
    protected XStream                    xmlResolver;
    protected MutableComponentRepository repository;
    private Logger                      logger                =
            LoggerFactory.getLogger(DefaultComponentResolver.class.getName());
    //应该用一个DM的merge/unmerge，但是现在这么做会失败，需要debug
    private Stack<DependencyManagement> dependencyManagements = new Stack<DependencyManagement>();

    public DefaultComponentResolver(MutableComponentRepository repository) {
        if (repository == null) {
            throw new NullPointerException("The repository can't be null");
        }
        this.repository = repository;
        customizeXstream();
    }

    protected void customizeXstream() {
        logger.trace("Customizing xstream engine");
        xmlResolver = new XStream();
        xmlResolver.alias("project", DefaultComponent.class);
        xmlResolver.alias("parent", Component.class, DefaultComponent.class);
        xmlResolver.alias("dependencyManagement", DependencyManagement.class);
        //xmlResolver.aliasType("parent", DefaultComponent.class);
        xmlResolver.aliasField("packaging", DefaultComponent.class, "type");

        String ignores = "optional release modelVersion prerequisites issueManagement ciManagement " +
                         "inceptionYear mailingLists developers contributors licenses scm build profiles " +
                         "repositories pluginRepositories reports reporting " +
                         "distributionManagement organization relativePath";
        for (String field : ignores.split("\\s")) {
            xmlResolver.omitField(DefaultComponent.class, field);
        }
        xmlResolver.alias("dependency", Dependency.class);
        xmlResolver.alias("exclusion", Exclusion.class);
        xmlResolver.alias("module", String.class);
        xmlResolver.aliasField("modules", DefaultComponent.class, "moduleNames");
        xmlResolver.registerLocalConverter(DefaultComponent.class, "properties", new MavenPropertiesConverter());
        logger.trace("Customized  xstream engine");
    }

    @Override
    public Component resolveComponent(Dependency dependency, InputStream pomDotXml)
            throws DependencyNotMeetException, InvalidComponentNameException {
        //由于流是外部传入的，遵循谁打开，谁关闭的原则，所以此地并不需要关闭之
        DefaultComponent component = (DefaultComponent) xmlResolver.fromXML(pomDotXml);
        DefaultComponent parent = (DefaultComponent) component.getParent();
        //处理属性的继承
        if (parent != null) {
            if(parent.getType() == null )
                parent.setType("pom");
            parent.splitVersionAndClassifier();
            if (component.getGroupId() == null) {
                component.setGroupId(parent.getGroupId());
            }
            if (component.getVersion() == null) {
                component.setVersion(parent.getVersion());
                component.setClassifier(parent.getClassifier());
            }else{
                component.splitVersionAndClassifier();
            }
        }
        if(!component.isSnapshot())
            component.setRelease(true);//缺省解析出来的都是release的
        if (component.getType() == null)
            component.setType("jar");//缺省为jar
        try {
            //验证组件关键信息是否都齐了
            component.validate();
            //把各个解析出来的组件存储到仓库中，因为在解析 sub module时，其reference parent时会需要
            repository.addComponent(component);
            //解析Parent信息
            parent = processParent(dependency, component, parent);
            //解析组件的基本动态属性，放在parent解析之后，这样可以获取到parent的属性
            component.interpolate();

            //处理 dependency management scope = import的dependency（需要merge)
            DependencyManagement dm = processDependencyManagement(component, parent);

            //验证依赖信息
            dependencyManagements.push(dm);
            try {
                processDependencies(dependency, component);
            } finally {
                dependencyManagements.pop();
            }
            //这个策略貌似不对，解析了父模块，难道一定要解析子模块？没这个道理
            // 以后如果有使用者有需要，则可以把这个功能开放到仓库接口上，变成一个高级功能，让调用者主动调用
            //解析子模块信息
            //processModules(component);

        } catch (DependencyNotMeetException e) {
            repository.removeComponent(component);
            throw e;
        } catch (InvalidComponentException e) {
            //InvalidParent: parent找不到
            //DependencyNotMeet: 依赖找不到
            //InvalidComponent: 子模块解析不成功
            // 均可能导致错误
            //此时，该组件就是一个垃圾，应该被删除
            repository.removeComponent(component);
            throw e;
        }

        logger.debug("Resolved  {} from input stream", component);
        return component;
    }

    protected DefaultComponent processParent(Dependency childDependency,
                                             DefaultComponent component,
                                             DefaultComponent parent)
            throws InvalidComponentNameException, DependencyNotMeetException {
        if (parent == null)
            return null;
        //指向parent的version可能包括动态变量，但此时还没有完全解析，所以对parent version进行单独解析
        String parentVersion = parent.getVersion();
        parentVersion = component.interpolate(parentVersion);
        // Replace with repository defined parent
        Dependency dependency = new Dependency(parent.getGroupId(), parent.getArtifactId(), parentVersion);
        dependency.setClassifier(parent.getClassifier());
        dependency.setType(parent.getType());
        dependency.setExclusions(childDependency.getExclusions());

        Component existParent;
        try {
            //此处并不需要把父组件的依赖也合并到当前子组件中
            //而是建立父子组件之间的关联关系，以后使用者可以遍历父子树建立完整的依赖关系图
            existParent = repository.resolveComponent(dependency);
            component.setParent(existParent);
            mergeDependencies(existParent, component);
            logger.trace("Attaching {} as parent of {}", existParent, component);
            return (DefaultComponent) existParent;
        } catch (DependencyNotMeetException e) {
            //这个依赖不满足的情况，可能是其他试错产生的，只有是对应parent依赖不满足才 on-demand的将这个临时parent加入仓库
            if (dependency.accept(e.getDependency())) {
                //部分开源组件的parent无法解析出来，那么就直接使用最简单的parent定义
                logger.debug("Demanding {} as parent, because of: {}", parent, e.getMessage());

                repository.addComponent(parent);
            } else {
                throw new DependencyNotMeetException(component, dependency, e);
            }
        }
        return parent;
    }

    DependencyManagement processDependencyManagement(DefaultComponent component, DefaultComponent parent) throws InvalidComponentNameException {
        DependencyManagement dm = component.getDependencyManagement();
        if( !dm.isEmpty() ){
            //TO avoid concurrent modifications
            Set<Dependency> dependencies = new HashSet<Dependency>(dm.getDependencies());
            for(Dependency d : dependencies){
                d.interpolate(component);
                if( "import".equalsIgnoreCase(d.getScope())){
                    try {
                        Component importing = repository.resolveComponent(d);
                        dm.merge(importing.getDependencyManagement());
                    } catch (DependencyNotMeetException e) {
                        logger.debug("Dependency with import scope is not supported fully {}", e.getMessage());
                    }
                }
            }
        }
        if( parent != null )
            dm.merge(parent.getDependencyManagement());
        return dm;
    }

    protected void mergeDependencies(Component parent, DefaultComponent component) {
        for (Dependency parentDefined : parent.getDependencies()) {
            //parent defined形如:
            /*    <dependency>
                    <groupId>jmock</groupId>
                    <artifactId>jmock</artifactId>
                    <version>${version.jmock}</version>
                    <scope>test</scope>
                  </dependency>
            */
            //而子对象定义的形如：
            /*
                <dependency>
                  <groupId>jmock</groupId>
                  <artifactId>jmock</artifactId>
                </dependency>
             */
            Dependency defined = component.getDependency(parentDefined);
            if (defined != null) {
                defined.merge(parentDefined);
            }
        }
        for (Dependency parentDefined : parent.getDependencyManagement().getDependencies()) {
            Dependency defined = component.getDependency(parentDefined);
            if (defined != null) {
                defined.merge(parentDefined);
            }
        }
    }

    protected void processDependencies(Dependency dependency, DefaultComponent component)
            throws InvalidComponentNameException, DependencyNotMeetException {
        if (component.getDependencies().isEmpty()) return;
        //验证依赖信息
        List<Dependency> dependencies = component.getDependencies();
        List<Component> dependedComponents = new ArrayList<Component>(dependencies.size());
        for (Dependency depended : dependencies) {
            if (dependency.exclude(depended)) {
                logger.trace("Skip excluded {}", depended);
                continue;
            }
            //继承depended的依赖排除
            if(dependency.hasExclusions()){
                depended.exclude(dependency.getExclusions());
            }
            //继承组件的依赖排除
            qualify(depended);
            if (!depended.isTest()) {//只要不是Test的，就都尝试解析
                try {
                    Component dependedComponent = repository.resolveComponent(depended);
                    dependedComponents.add(dependedComponent);
                } catch (DependencyNotMeetException e) {
                    if( e.getComponent() == null ) e.setComponent(component);
                    //但只有必选的，以及是compile time的找不到才抛出异常
                    if (!depended.isOptional() && depended.isCompile()) {
                        if(!DefaultComponent.isApplication(depended.getGroupId())){
                            //只有应用组件缺失依赖时抛错，第三方组件缺失时不抛错
                            if( e.getComponent() == component )
                                throw e;
                            else
                                throw new DependencyNotMeetException(component, depended, e);
                        }
                    } else{
                        logger.trace("Can't resolve {} dependency {}", depended.getScope(), depended);
                    }
                }
            } else {
                logger.trace("Skip {} dependency {}", depended.getScope(), depended);
            }
        }
        component.setDependedComponents(dependedComponents);
    }

    private void qualify(Dependency depended) {
        for (DependencyManagement dm : dependencyManagements) {
            dm.qualify(depended);
        }
    }

    @Override
    public Component resolveComponent(final Dependency dependency, Resource resource)
            throws InvalidComponentNameException, DependencyNotMeetException {
        logger.debug("Resolving {} from {}", dependency, resource);
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        File originFile;
        try {
            originFile = resource.getFile();
        } catch (IOException e) {
            originFile = null;
        }
        String fileName;
        if( originFile != null ) {
            fileName = originFile.getAbsolutePath();
        }else{
            fileName = resource.getDescription();
        }
        DefaultComponent comp;
        InputStream stream = null;
        if (fileName.endsWith(".pom")) {
            try {
                stream = resource.getInputStream();
                comp = (DefaultComponent) resolveComponent(dependency, stream);
                comp.setUnderlyingResource(resource);
                if (!comp.isAggregating()) {
                    File jarFile = digJarFilePath(fileName);
                    if (jarFile != null) {
                        ComponentJarResource jarResource = new ComponentJarResource(dependency, relativePath(jarFile));
                        comp.setResource(jarResource);
                    } else {
                        logger.warn("Can't find jar file for {}", comp);
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("The pom file does not exist: " + fileName);
            } finally {
                try {
                    if (stream != null)
                        stream.close();
                } catch (IOException ex) {/**/}
            }
        } else {
            ComponentJarResource jarResource = new ComponentJarResource(dependency, relativePath(fileName));
            try {
                stream = jarResource.getPomStream();
                comp = (DefaultComponent) resolveComponent(dependency, stream);
                comp.setUnderlyingResource(resource);
                comp.setResource(jarResource);
                if( dependency.getVersion() == null ){
                    dependency.setVersion(comp.getVersion());
                }
            } catch (IOException e) {
                throw new InvalidComponentException(dependency.getGroupId(), dependency.getArtifactId(),
                                                    dependency.getVersion(), "jar",
                                                    "Can't resolve pom.xml: " + e.getMessage());
            } finally {
                try {
                    if (stream != null)
                        stream.close();
                } catch (IOException ex) {/**/}
            }
        }
        if (comp.getType() == null)
            comp.setType(dependency.getType());

        if (!dependency.accept(comp))
            throw new InvalidComponentNameException("The component file name: " + fileName +
                                                    " conflict with its inner pom: " + comp.toString());

        if( !(currentCL instanceof ManipulateClassLoader)){
            throw new ApplicationConfigurationException("Current thread's Class Loader should implements ManipulateClassLoader");
        }
        ManipulateClassLoader mcl = (ManipulateClassLoader) currentCL;
        //将该组件依赖的第三方包添加到当前Class Loader的ucp中
        // 可能是MainClassLoader，也可能是ExtensionClassLoader，或者其他
        mcl.addURLs(comp.getLibURLs());
        //将该组件自身的url添加到当前Class Loader中
        if( comp.getResource() != null ){
            try {
                URL compUrl = comp.getResource().getURL();
                URLClassLoader singleURLCL = new URLClassLoader(new URL[]{compUrl}, ClassLoader.getSystemClassLoader());
                //让component jar resource的bind cl为仅能获取单个url的cl
                comp.getResource().setClassLoader(singleURLCL);
                mcl.addURL(compUrl);
            } catch (MalformedURLException e) {
                throw new InvalidComponentException(comp.getGroupId(), comp.getArtifactId(), comp.getVersion(), comp.getType(),
                                                    "Can't get component url " + e.getMessage());
            }
        }
        comp.setClassLoader(mcl);
        return comp;
    }

    protected File digJarFilePath(String fileName) {
        String[] folders = {"lib", "repository", "boot"};
        String home = repository.getHome();
        for (String folder : folders) {
            String path = String.format("%s/%s/%s", home, folder, fileName);
            path = path.replace(".pom", ".jar");
            File file = new File(path);
            if (file.exists()) return file;
        }
        return null;
    }


}
