/**
 * @author XiongJie, Date: 13-9-13
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.container.ComponentResolver;
import net.happyonroad.component.container.MutableComponentRepository;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.exception.ResourceNotFoundException;
import net.happyonroad.component.core.support.ComponentJarResource;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.component.core.support.Dependency;
import net.happyonroad.util.FilenameFilterBySuffix;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static net.happyonroad.util.LogUtils.banner;

/**
 * 组件仓库
 * 请注意，该对象的工作依赖系统环境变量 app.home
 */
public class DefaultComponentRepository
        implements MutableComponentRepository {
    private static FilenameFilter jarFilter = new FilenameFilterBySuffix(".jar");
    private static FilenameFilter pomFilter = new FilenameFilterBySuffix(".pom");

    private static DefaultComponentRepository sharedInstance;

    private Logger logger = LoggerFactory.getLogger(DefaultComponentRepository.class);

    private File                      home;
    private String                    mainComponentId;
    private Set<Component>            components;
    private Map<Dependency, Resource> cache;

    /*package*/ ComponentResolver resolver;

    /**
     * 构建一个缺省组件仓库
     *
     * @param home 应用根路径
     */
    public DefaultComponentRepository(String home) {
        sharedInstance = this;
        this.home = new File(home);
        File libFolder = new File(home, "lib");
        if (!libFolder.exists()) {
            try {
                FileUtils.forceMkdir(libFolder);
            } catch (IOException e) {
                throw new IllegalArgumentException("The lib folder [" + libFolder.getPath()
                                                   + "] not exist and can't be auto created: " + e.getMessage());
            }
        }
        components = new HashSet<Component>();
        cache = new HashMap<Dependency, Resource>();
        resolver = new DefaultComponentResolver(this);
    }

    @Override
    public String getMainComponentId() {
        return mainComponentId;
    }
// ------------------------------------------------------------
    //     本身的Lifecycle方法
    // ------------------------------------------------------------


    /** 启动时预加载 lib目录, lib/poms 下所有的组件信息 */
    public void start() {
        logger.info(banner("Scanning jars"));
        //先寻找 boot/*.jar，将其预加载为component
        File bootFolder = new File(home, "boot");
        try {
            scanJars(bootFolder);
        } catch (Exception e) {
            logger.error("Failed to scan {} dir: {}", bootFolder, e.getMessage());
        }

        //再寻找 lib/*.jar，将其预加载为component
        File libFolder = new File(home, "lib");
        try {
            scanJars(libFolder);
        } catch (Exception e) {
            logger.error("Failed to scan {} dir: {}", libFolder, e.getMessage());
        }
        File poms = new File(libFolder, "poms");
        if( poms.exists() )
            try {
                //而后再遍历lib/poms下面的pom.xml，把packaging = pom 的父pom预加载进来
                scanPoms(poms);
            } catch (Exception e) {
                logger.error("Failed to scan {} dir: {}", poms, e.getMessage());
            }
        File pomsJar = new File(libFolder, "poms.jar");
        if( pomsJar.exists())
            try {
                //而后再遍历lib/poms下面的pom.xml，把packaging = pom 的父pom预加载进来
                scanPomsJar(pomsJar);
            } catch (Exception e) {
                logger.error("Failed to scan {} dir: {}", poms, e.getMessage());
            }

        //最后还要扫描扩展仓库目录 repository/*.jar
        File repositoryFolder = new File(home, "repository");
        try {
            scanJars(repositoryFolder);
        } catch (Exception e) {
            logger.error("Failed to scan {} dir: {}", repositoryFolder, e.getMessage());
        }
        poms = new File(repositoryFolder, "poms");
        if( poms.exists() )
            try {
                //而后再遍历repository/poms下面的pom.xml，把不存在对应jar的 group pom预加载进来
                scanPoms(poms);
            } catch (Exception e) {
                logger.error("Failed to scan {} dir: {}", poms, e.getMessage());
            }
        pomsJar = new File(repositoryFolder, "poms.jar");
        if( pomsJar.exists())
            try {
                //而后再遍历lib/poms下面的pom.xml，把packaging = pom 的父pom预加载进来
                scanPomsJar(pomsJar);
            } catch (Exception e) {
                logger.error("Failed to scan {} dir: {}", poms, e.getMessage());
            }
        // 扫描 repository/lib 下的jar，这里面存放的是扩展包的依赖
        File repositoryLibFolder = new File(repositoryFolder, "lib");
        try {
            scanJars(repositoryLibFolder);
        } catch (Exception e) {
            logger.error("Failed to scan {} dir: {}", repositoryFolder, e.getMessage());
        }
        logger.info(banner("Scanned  jars"));
    }

    private void scanJars(File folder) throws InvalidComponentNameException, IOException {
        File[] jars = folder.listFiles(jarFilter);
        if (jars == null)
            jars = new File[0]; /*也可能在boot或lib目录下没有jar*/
        logger.debug("Scanning {}", folder.getAbsolutePath());
        for (File jar : jars) {
            if(jar.getPath().endsWith("-sources.jar")){
                logger.trace("Skip sources {}", jar.getPath());
                continue;
            }
            if( jar.getName().equalsIgnoreCase("wrapper.jar")){
                logger.trace("Skip wrapper.jar");
                continue;
            }
            if( jar.getName().equalsIgnoreCase("poms.jar")){
                logger.trace("Skip poms.jar");
                continue;
            }
            Dependency dependency = Dependency.parse(jar.getName());
            //先放到cache里面，避免构建 component jar resource时无法寻址
            cache.put(dependency, new FileSystemResource(jar));
            InputStream stream = null;
            ComponentJarResource resource = null;
            try {
                resource = new ComponentJarResource(dependency, jar.getName());
                stream = resource.getPomStream();
                //只有存在 pom.xml 的jar包才直接解析
                //而不存在 pom.xml 的jar包，需要依赖 poms里面的定义进行解析
            } catch (IOException e) {
                cache.remove(dependency);
            } catch (ResourceNotFoundException e) {
                //Not cache it by the concrete jar
                cache.remove(dependency);
                //logger.error("Error while read :" + jar.getPath(), e);
            } finally {
                try {
                    if(resource != null ) resource.close();
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    logger.error("Error while close:" + jar.getPath(), e);
                }
            }
            logger.trace("Mapping {} -> {}", dependency, jar);
        }
        logger.debug("Scanned  {}", folder.getAbsolutePath());

    }

    private void scanPoms(File poms) throws InvalidComponentNameException {
        logger.trace("Scan {}", poms.getAbsolutePath());
        File[] pomFiles = poms.listFiles(pomFilter);
        if(pomFiles == null) pomFiles = new File[0];
        for (File pomFile : pomFiles) {
            Dependency dependency = Dependency.parse(pomFile.getName());
            //如果lib/*.jar已经设置了相应的依赖解析路径，那么不需要在这里再重新设置
            //注意，Dependency的equals和hashCode方法被重写过，所以，指向 jar和pom的依赖是一回事
            if (cache.get(dependency) != null) continue;
            cache.put(dependency, new FileSystemResource(pomFile));
            logger.trace("Mapping {} -> {}", dependency, pomFile);
        }
    }

    private void scanPomsJar(File pomsJar) throws InvalidComponentNameException {
        logger.trace("Scan {}", pomsJar.getAbsolutePath());
        try {
            JarFile jarFile = new JarFile(pomsJar);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.getName().endsWith(".pom")){
                    String baseName = FilenameUtils.getBaseName(entry.getName());
                    Dependency dependency = Dependency.parse(baseName);
                    if (cache.get(dependency) != null) continue;
                    cache.put(dependency, new InputStreamResource(jarFile.getInputStream(entry), entry.getName()));
                    logger.trace("Mapping {} -> {}", dependency, jarFile.getName() + "!/" + entry.getName());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't scan poms from " + pomsJar.getAbsolutePath());
        }

    }

    public void stop() {
        logger.info(banner("{} Stopping", getClass().getSimpleName()));
        cache.clear();
        //卸载application/service是开发者的责任，应该通过调用 LaunchEnvironment#unload(mainComponent)方法实现
        for (Component component : components) {
            ComponentResource resource = component.getResource();
            if (resource != null) resource.close();
        }
        components.clear();
        logger.info(banner("{} Stopped", getClass().getSimpleName()));
    }

    // ------------------------------------------------------------
    //     对外业务接口方法
    // ------------------------------------------------------------

    @Override
    public Component resolveComponent(Dependency dependency)
            throws InvalidComponentNameException, DependencyNotMeetException {
        List<Component> meets = resolveComponents(dependency);
        if (meets.isEmpty()) {
            Set<Dependency> dependencies = new HashSet<Dependency>(cache.keySet());
            for (Dependency dep : dependencies) {
                if (dependency.accept(dep)) {
                    Resource resource = cache.get(dep);
                    meets.add(resolver.resolveComponent(dependency, resource));
                    //cache.remove(dep);
                }
            }
        }
        if (meets.isEmpty()) {
            logger.trace("Can't find {} in current repository", dependency);
            throw new DependencyNotMeetException(dependency);
        }
        return meets.get(0);
    }

    @Override
    public Component resolveComponent(String strDependency)
            throws DependencyNotMeetException, InvalidComponentNameException {
        if( this.mainComponentId == null ) mainComponentId = strDependency;
        Dependency dependency = Dependency.parse(strDependency);
        return resolveComponent(dependency);
    }


    /**
     * 从当前已经解析的组件中找到符合依赖的组件列表，这不会触发实际解析动作
     *
     * @param dependency 依赖信息
     * @return 组件列表
     */
    @Override
    public List<Component> resolveComponents(Dependency dependency) {
        logger.trace("Finding   {}", dependency);
        List<Component> meets = new ArrayList<Component>();
        for (Component component : components) {
            if (dependency.accept(component)) {
                meets.add(component);
            }
        }
        if (meets.isEmpty()) {
            logger.trace("Missing   {}", dependency);
        } else {
            logger.trace("Found     {} {} components", dependency, meets.size());
        }
        //对所有满足的依赖进行排序
        Collections.sort(meets);
        //排序的原则是越高版本越好
        Collections.reverse(meets);
        return meets;
    }

    @Override
    public Set<Component> getComponents() {
        return Collections.unmodifiableSet(components);
    }

    @Override
    public void sortCandidates(File[] candidateComponentJars)
            throws DependencyNotMeetException, InvalidComponentNameException {
        final List<Component> components = new ArrayList<Component>(candidateComponentJars.length);
        final Map<File, Component> mapping = new HashMap<File, Component>();
        for (File file : candidateComponentJars) {
            Component component = resolveComponent(file.getName());
            components.add(component);
            mapping.put(file, component);
        }
        sortComponents(components);
        Arrays.sort(candidateComponentJars, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return positionOf(f1) - positionOf(f2);
            }

            private int positionOf(File file) {
                Component component = mapping.get(file);
                return components.indexOf(component);
            }
        });
    }

    /**
     * 基于依赖关系进行排序，最多被依赖的组件排在最前面，没有被依赖的排在最后面
     * <pre>
     * 这个排序算法不能基于java的quick sort，默认快速排序，排到前面的，不会再和后面的继续比较
     *
     * 基本的算法原理是：
     * 先为components集合构建一个相互之间的依赖关系，map结构
     *
     * </pre>
     * @param components 被排序的组件
     */
    @Override
    public void sortComponents(List<Component> components) {
        //将组件集合之间的依赖关系以map的形式展现出来
        Map<Component, List<Component>> dependencies = dependsMap(components);
        //清空待排序的components，用于做结果的容器
        components.clear();
        //基于依赖关系进行处理
        arrange(components, dependencies);
    }

    Map<Component, List<Component>> dependsMap(List<Component> components) {
        Map<Component, List<Component>> dependencies = new HashMap<Component, List<Component>>();
        for (Component component : components) {
            List<Component> depends = new ArrayList<Component>();
            for (Component other : components) {
                if( component == other ) continue;
                if( component.dependsOn(other) )
                    depends.add(other);
            }
            dependencies.put(component, depends);
        }
        return dependencies;
    }

    void arrange(List<Component> result, Map<Component, List<Component>> dependencies) {
        Iterator<Map.Entry<Component, List<Component>>> it = dependencies.entrySet().iterator();
        boolean cycled = true;
        while (it.hasNext()) {
            Map.Entry<Component, List<Component>> entry = it.next();
            if( entry.getValue().isEmpty() ){
                cycled = false;
                //从依赖数组中移除当前这个没有依赖任何其他组件的对象关系
                it.remove();
                //并将对象放到结果集合头部
                result.add(entry.getKey());
                //而后将所有其他的依赖关系中指向该记录的清除掉
                for (List<Component> list : dependencies.values()) {
                    list.remove(entry.getKey());
                }
            }
        }
        if( cycled ) throw new IllegalArgumentException("There are cycle dependencies in: "
                                                        + Arrays.toString(dependencies.keySet().toArray()));
        if( !dependencies.isEmpty() ) arrange(result, dependencies);
    }

    // ------------------------------------------------------------
    //     提供给内部实现者
    // ------------------------------------------------------------
    @Override
    public void addComponent(Component component) {
        logger.debug("Register  {} into repository", component);
        components.add(component);
    }

    @Override
    public void removeComponent(Component component) {
        logger.trace("Removing {} from repository", component);
        components.remove(component);
    }

    @Override
    public boolean isApplication(String groupId) {
        return DefaultComponent.isApplication(groupId);
    }

    @Override
    public String getHome() {
        return home.getPath();
    }


    public static DefaultComponentRepository getRepository() {
        return sharedInstance;
    }
}
