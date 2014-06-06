/**
 * @author XiongJie, Date: 13-8-28
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.Versionize;
import net.happyonroad.component.core.exception.InvalidComponentException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h2>组件对象</h2>
 * <p/>
 * 从pom.xml中解析出来，包括Group、Artifact、Version、Dependencies等信息
 * 这个类其实是对Maven Artifact的一种简化
 */
@SuppressWarnings("unused")
@ManagedResource(description = "Spring Component")
public class DefaultComponent implements Component, SelfNaming {
    private static       Logger   logger          = LoggerFactory.getLogger(DefaultComponent.class.getName());
    private static final Pattern  INTERPOLATE_PTN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final String[] ATTRIBUTE_NAMES = new String[]{"groupId", "artifactId", "version", "type",
                                                                 "classifier", "release", "name", "description",
                                                                 "url"};

    static {
        Arrays.sort(ATTRIBUTE_NAMES);
    }


    private Component parent;

    private String groupId;

    private String artifactId;

    private String type;//pom or jar, war, rar, and so on

    private String classifier;//snapshot, release, javadoc, source, java4, java5, java6, or other feature...

    private File                 file;//对应的实际文件
    private ComponentResource    resource;//对应的资源文件
    //DependencyManagement声明的是所有可能的依赖，但解析过程中，不应该触发它们被解析
    private DependencyManagement dependencyManagement;
    //这里声明的实际的依赖，可以引用或参考/继承 dependencyManagement 声明的依赖
    private List<Dependency>     dependencies;//依赖信息解析成为一堆字符串
    private List<Component>      dependedComponents;//依赖信息解析成为一堆字符串
    private List<String>         moduleNames;       //子模块，子项目，子组件
    private List<Component>      modules;       //子模块，子项目，子组件

    private String version;//核心版本，如: 1.0.0

    private boolean release = true;

    private String name, description, url;

    private Properties         properties;
    private ClassLoader        classLoader;
    private ApplicationContext application;

    // XStream Reflection 时并不需要提供一个缺省构造函数


    /**
     * 根据依赖里面定义的信息构建一个组件实例
     *
     * @param from 指定的组件信息，并不是指构建一个带有这个依赖的组件
     */
    public DefaultComponent(Versionize from) {
        this(from.getGroupId(), from.getArtifactId(), from.getVersion(), from.getClassifier(), from.getType());
    }

    public DefaultComponent(String groupId, String artifactId, String version, String classifier, String type) {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;

        this.classifier = classifier;

        this.type = type;

        validateIdentity();

        this.properties = new Properties();
    }

    private void validateIdentity() {
        if (empty(groupId)) {
            throw new InvalidComponentException(groupId, artifactId, getVersion(), type,
                                                "The groupId cannot be empty.");
        }

        if (artifactId == null) {
            throw new InvalidComponentException(groupId, "null", getVersion(), type,
                                                "The artifactId cannot be empty.");
        }

        if (type == null) {
            throw new InvalidComponentException(groupId, artifactId, getVersion(), type, "The type cannot be empty.");
        }

        if (version == null) {
            throw new InvalidComponentException(groupId, artifactId, getVersion(), type,
                                                "The version cannot be empty.");
        }
    }

    private boolean empty(String value) {
        return (value == null) || (value.trim().length() < 1);
    }

    @ManagedAttribute
    @Override
    public String getGroupId() {
        return groupId;
    }

    @ManagedAttribute
    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @ManagedAttribute
    @Override
    public String getVersion() {
        return version;
    }

    @ManagedAttribute
    @Override
    public String getClassifier() {
        return classifier;
    }

    public boolean hasClassifier() {
        return classifier != null && !"".equalsIgnoreCase(classifier.trim());
    }

    @ManagedAttribute
    @Override
    public String getType() {
        return type;
    }

    @ManagedAttribute
    public String getName() {
        return name;
    }

    @ManagedAttribute
    @Override
    public String getDisplayName() {
        return name == null ? getId() : name;
    }

    @ManagedAttribute
    public String getDescription() {
        return description;
    }

    @ManagedAttribute
    public String getUrl() {
        return url;
    }

    public void setVersion(String version) {
        this.version = version;
        this.splitVersionAndClassifier();
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
        //Classifier设置之后，就要自动设置release属性
        if (isSnapshot()) setRelease(false);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setResource(ComponentResource resource) {
        this.resource = resource;
    }

    @ManagedAttribute
    @Override
    public File getFile() {
        return file;
    }

    @ManagedAttribute
    @Override
    public URL getFileURL() {
        if (file == null) {
            return null;
        }
        try {
            if (isAggregating()) {
                return null;
            } else if (file.isFile()) {
                if (file.getName().endsWith(".pom")) {
                    String fileName = file.getName();
                    fileName = fileName.replaceFirst("\\.pom$", ".jar");
                    File jarFile = new File(file.getParentFile().getParent(), fileName);
                    if (jarFile.exists())
                        return jarFile.toURI().toURL();
                    else
                        return null;
                } else if (file.getName().endsWith(".jar")) {
                    return file.toURI().toURL();
                } else {
                    //unknown file type, return file url as default
                    return file.toURI().toURL();
                }
            } else { //folder
                return file.toURI().toURL();
            }
        } catch (MalformedURLException e) {
            /*ignore*/
            return null;
        }
    }

    public ComponentResource getResource() {
        return resource;
    }
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    @ManagedAttribute
    @Override
    public String getId() {
        return getDependencyConflictId();
    }

    @Override
    public ObjectName getObjectName() {
        String name = getBriefId();
        try {
            return new ObjectName("spring.components:name=" + name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Can't create component object name:" + name);
        }
    }

// ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getGroupId() != null) {
            sb.append(getGroupId());
            sb.append(".");
        }
        appendArtifactTypeClassifierString(sb);
        return sb.toString();
    }

    public String getDependencyConflictId() {
        StringBuffer sb = new StringBuffer();
        sb.append(getGroupId());
        sb.append(".");
        appendArtifactTypeClassifierString(sb);
        return sb.toString();
    }

    private void appendArtifactTypeClassifierString(StringBuffer sb) {
        sb.append(getArtifactId());
        sb.append("-").append(getVersion() == null ? "<*>" : getVersion());
        if (hasClassifier()) {
            sb.append("-");
            sb.append(getClassifier());
        }
        sb.append(".");
        sb.append(getType() == null ? "<*>" : getType());
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + (type == null ? "<*>".hashCode() : type.hashCode());
        if (version != null) {
            result = 37 * result + version.hashCode();
        }
        result = 37 * result + (classifier != null ? classifier.hashCode() : 0);
        return result;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Component)) {
            return false;
        }

        Component a = (Component) o;

        if (!a.getGroupId().equals(groupId)) {
            return false;
        } else if (!a.getArtifactId().equals(artifactId)) {
            return false;
        } else if (!a.getVersion().equals(version)) {
            return false;
        } else if (!a.getType().equals(type)) {
            return false;
        } else if (a.getClassifier() == null ? classifier != null : !a.getClassifier().equals(classifier)) {
            return false;
        }

        // We don't consider the version range in the comparison, just the resolved version

        return true;
    }


    public int compareTo(@SuppressWarnings("NullableProblems") Component a) {
        int result = groupId.compareTo(a.getGroupId());
        if (result == 0) {
            result = artifactId.compareTo(a.getArtifactId());
            if (result == 0) {
                result = type.compareTo(a.getType());
                if (result == 0) {
                    if (classifier == null) {
                        if (a.getClassifier() != null) {
                            result = 1;
                        }
                    } else {
                        if (a.getClassifier() != null) {
                            result = classifier.compareTo(a.getClassifier());
                        } else {
                            result = -1;
                        }
                    }
                    if (result == 0) {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = version.compareTo(a.getVersion());
                    }
                }
            }
        }
        return result;
    }

    public Component getParent() {
        return parent;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    public List<Dependency> getDependencies() {
        if (dependencies == null) dependencies = new ArrayList<Dependency>(0);
        return dependencies;
    }

    @Override
    public Dependency getDependency(Dependency dependency) {
        if (dependencies == null)
            return null;
        for (Dependency candidate : dependencies) {
            if (dependency.conflict(candidate))
                return candidate;
        }
        return null;
    }

    public DependencyManagement getDependencyManagement() {
        if (dependencyManagement == null)
            dependencyManagement = new DependencyManagement();
        return dependencyManagement;
    }

    public List<Component> getDependedComponents() {
        if (dependedComponents == null) dependedComponents = new ArrayList<Component>(0);
        return dependedComponents;
    }

    @Override
    public Set<Dependency> getAllDependencies() {
        Set<Dependency> all = new HashSet<Dependency>();
        if (getDependencies() != null) {
            all.addAll(getDependencies());
        }
        if (this.parent != null) {
            all.addAll(this.parent.getAllDependencies());
        }
        return all;
    }

    public Set<Component> getAllDependedComponents() {
        Set<Component> all = new HashSet<Component>();
        if (dependedComponents != null) {
            for (Component depended : dependedComponents) {
                all.add(depended);
                all.addAll(depended.getAllDependedComponents());
            }
        }
        if (this.parent != null) {
            all.addAll(this.parent.getAllDependedComponents());
        }
        return all;
    }

    @ManagedAttribute
    @Override
    public Set<URL> getDependedPlainURLs() {
        Set<URL> plainUrls = new HashSet<URL>();
        if (getParent() != null) {
            plainUrls.addAll(getParent().getDependedPlainURLs());
        }
        List<Component> depends = getDependedComponents();
        for (Component depend : depends) {
            if (depend.isPlain() && depend.getFileURL() != null) {
                plainUrls.add(depend.getFileURL());
            }
            plainUrls.addAll(depend.getDependedPlainURLs());
        }
        return plainUrls;
    }


    public void setDependedComponents(List<Component> dependedComponents) {
        this.dependedComponents = dependedComponents;
    }

    @ManagedAttribute
    public List<String> getModuleNames() {
        return moduleNames;
    }

    public List<Component> getModules() {
        return modules;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void setModuleNames(List<String> modules) {
        this.moduleNames = modules;
    }

    public void setModules(List<Component> modules) {
        this.modules = modules;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @ManagedAttribute
    @Override
    public boolean isSnapshot() {
        return SNAPSHOT_VERSION.equalsIgnoreCase(getClassifier());
    }

    public void setResolvedVersion(String version) {
        this.version = version;
        // retain baseVersion
    }

    public void setRelease(boolean release) {
        this.release = release;
    }

    @ManagedAttribute
    @Override
    public boolean isRelease() {
        return release;
    }

    @ManagedAttribute
    @Override
    public boolean isAggregating() {
        return "pom".equals(getType());
    }

    @ManagedAttribute
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    //其实如果以当前project为this，把env, system作为上下文， 通过对象图的方式提供变量索引，实现起来更好
    @Override
    public String resolveVariable(String key) {
        if (key.startsWith("project.")) {
            return resolveVariable(key.substring("project.".length()));
        } else if (key.startsWith("pom.")) {
            return resolveVariable(key.substring("pom.".length()));
        } else if (key.startsWith("parent.")) {
            if (parent == null)
                return null;
            return parent.resolveVariable(key.substring("parent.".length()));
        } else if (key.startsWith("env.")) {
            return System.getenv(key.substring("env.".length()));
        }
        if ("packaging".equals(key))
            key = "type";
        if (Arrays.binarySearch(ATTRIBUTE_NAMES, key) >= 0) {
            try {
                Field field = getClass().getDeclaredField(key);
                return String.valueOf(field.get(this));
            } catch (Exception e) {
                return null;
            }
        } else if (System.getProperty(key) != null) {
            return System.getProperty(key);
        } else if (properties != null) {
            String property = properties.getProperty(key);
            if (property == null && parent != null) {
                property = parent.resolveVariable(key);
            }
            return property;
        } else {
            // delegate parent to resolve it
            if (parent == null) return null;
            return parent.resolveVariable(key);
        }
    }

    @ManagedAttribute
    @Override
    public boolean isPlain() {
        return !isApplication() || getResource() == null;
    }

    @Override
    public String getManifestAttribute(String attributeName) {
        if (this.resource == null)
            return null;
        return this.resource.getManifest().getMainAttributes().getValue(attributeName);
    }

    public void validate() throws InvalidComponentNameException {
        if (groupId == null)
            throw new InvalidComponentNameException("The component group id can't be null");
        if (artifactId == null)
            throw new InvalidComponentNameException("The component artifact id can't be null");
        if (type == null)
            throw new InvalidComponentNameException("The component type can't be null");
    }


    public void splitVersionAndClassifier() {
        String[] versionAndClassifier = Dependency.splitClassifierFromVersion(version, new StringBuilder());
        this.version = versionAndClassifier[0];
        if (versionAndClassifier[1].length() > 0) setClassifier(versionAndClassifier[1]);
    }

    public void interpolate() {
        if (groupId != null)
            this.groupId = interpolate(groupId);
        if (artifactId != null)
            this.artifactId = interpolate(artifactId);
        //暂时主要处理一些启动相关的关键属性，如version
        if (version != null)
            this.setVersion(interpolate(version));
        if (name != null)
            this.name = interpolate(name);
        if (classifier != null)
            this.classifier = interpolate(classifier);
        if (description != null)
            this.description = interpolate(description);
        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                dependency.interpolate(this);
            }
        }
        if (dependencyManagement != null) {
            for (Dependency dependency : dependencyManagement.getDependencies()) {
                dependency.interpolate(this);
            }
        }
    }

    public String interpolate(String origin) {
        Matcher m = INTERPOLATE_PTN.matcher(origin);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            String replacement = resolveVariable(variable);
            if (replacement == null) {
                if (isApplication(groupId))
                    throw new InvalidComponentException(this.groupId, this.artifactId, this.version, this.type,
                                                        "Can't interpolate variable [" + variable + "]");
                else {
                    logger.warn("Can't interpolate variable [" + variable + "] for " + this);
                    continue;
                }
            }
            //解析出来的变量可能还需要再解析
            if (INTERPOLATE_PTN.matcher(replacement).find()) {
                replacement = interpolate(replacement);
            }
            try {
                m.appendReplacement(sb, replacement);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();//just for catch it to debug
                throw e;
            }
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    @Override
    public boolean isApplication() {
        return DefaultComponent.isApplication(getGroupId());
    }

    static public boolean isApplication(String groupId) {
        //这个属性可以通过设置 app.prefix系统属性进行干预，支持多个prefix用分号分隔
        //当符合该条件的模块没有被解析出来时，系统将会停止解析，抛出异常；
        //而不符合该条件的模块，往往是第三方未用到的模块，解析失败不应该停止
        String prefixes = System.getProperty("app.prefix", "dnt.");
        String[] strings = prefixes.split(";");
        for (String prefix : strings) {
            if (groupId.startsWith(prefix)) return true;
        }
        return false;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void perform(Runnable job) throws Exception {
        ClassLoader originCl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = this.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            job.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originCl);
        }
    }

    public <T> T perform(Callable<T> job) throws Exception {
        ClassLoader originCl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = this.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            return job.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originCl);
        }
    }

    @Override
    public boolean dependsOn(Component another) {
        Set<Component> depends = getAllDependedComponents();
        for (Component depended : depends) {
            if (depended.equals(another))
                return true;
        }
        return false;
    }

    @Override
    public String getBriefId() {
        return getGroupId() + "." + getArtifactId() + "-" + getVersion();
    }

    public ApplicationContext getApplication() {
        return application;
    }

    public void setApplication(ApplicationContext application) {
        this.application = application;
    }
}


