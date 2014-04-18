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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static net.happyonroad.util.LogUtils.banner;

/**
 * 组件仓库
 * 请注意，该对象的工作依赖系统环境变量 app.home
 */
public class DefaultComponentRepository implements MutableComponentRepository, SmartLifecycle {
    private static FilenameFilter jarFilter = new FilenameFilterBySuffix(".jar");
    private static FilenameFilter pomFilter = new FilenameFilterBySuffix(".pom");

    private Logger logger = LoggerFactory.getLogger(DefaultComponentRepository.class);

    private File                  home;
    private Set<Component>        components;
    private Map<Dependency, File> cache;

    /*package*/ ComponentResolver resolver;
    private boolean running;

    /**
     * 构建一个缺省组件仓库
     *
     * @param home 应用根路径
     */
    public DefaultComponentRepository(String home) {
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
        cache = new HashMap<Dependency, File>();
        resolver = new DefaultComponentResolver(this);
    }

    // ------------------------------------------------------------
    //     本身的Lifecycle方法
    // ------------------------------------------------------------

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                logger.error("Failed to run callback", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 5;
    }

    /** 启动时预加载 lib目录, lib/poms 下所有的组件信息 */
    public void start()  {
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
        try {
            //而后再遍历lib/poms下面的pom.xml，把不存在对应jar的 group pom预加载进来
            scanPoms(poms);
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
        try {
            //而后再遍历repository/poms下面的pom.xml，把不存在对应jar的 group pom预加载进来
            scanPoms(poms);
        } catch (Exception e) {
            logger.error("Failed to scan {} dir: {}", poms, e.getMessage());
        }
        this.running = true;
        logger.info(banner("Scanned jars"));
    }

    private void scanJars(File folder) throws InvalidComponentNameException, IOException {
        File[] jars = folder.listFiles(jarFilter);
        if (jars == null)
            jars = new File[0]; /*也可能在boot或lib目录下没有jar*/
        logger.debug("Scanning {}", folder.getAbsolutePath());
        for (File jar : jars) {
            if(jar.getPath().endsWith("-sources.jar")){
                logger.trace("Skip sources {}", jar.getPath());
            }
            Dependency dependency = Dependency.parse(jar.getName());
            ComponentJarResource resource = new ComponentJarResource(jar);
            InputStream stream = null;
            try {
                stream = resource.getPomStream();
                //只有存在 pom.xml 的jar包才直接解析
                //而不存在 pom.xml 的jar包，需要依赖 poms里面的定义进行解析
                cache.put(dependency, jar);
            } catch (ResourceNotFoundException e) {
                //Not cache it by the concrete jar
                //logger.error("Error while read :" + jar.getPath(), e);
            } finally {
                try {
                    resource.close();
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
            cache.put(dependency, pomFile);
            logger.trace("Mapping {} -> {}", dependency, pomFile);
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
                    File file = cache.get(dep);
                    meets.add(resolver.resolveComponent(dependency, file));
                    //cache.remove(dep);
                }
            }
        }
        if (meets.isEmpty()) {
            logger.trace("Can't find {} in current repository", dependency);
            throw new DependencyNotMeetException(dependency, "in current repository");
        }
        return meets.get(0);
    }

    @Override
    public Component resolveComponent(String strDependency)
            throws DependencyNotMeetException, InvalidComponentNameException {
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
}
