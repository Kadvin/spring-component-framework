/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.container.AppLauncher;
import net.happyonroad.component.container.ComponentLoader;
import net.happyonroad.component.container.Executable;
import net.happyonroad.component.container.LaunchEnvironment;
import net.happyonroad.component.container.event.ContainerEvent;
import net.happyonroad.component.container.event.ContainerStartedEvent;
import net.happyonroad.component.container.event.ContainerStoppingEvent;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentContext;
import net.happyonroad.component.core.FeatureResolver;
import net.happyonroad.component.core.exception.DependencyNotMeetException;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.spring.context.ContextUtils;
import net.happyonroad.util.LogUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

import static org.apache.commons.lang.time.DurationFormatUtils.formatDurationHMS;

/**
 * 为了让Launch静态程序可以被定制化，从原 Plexus/Launch中分离出来的对象
 */
public class DefaultLaunchEnvironment implements LaunchEnvironment {
    public static final int MODE_START = 1;
    public static final int MODE_STOP  = -1;

    private Logger logger = LoggerFactory.getLogger(AppLauncher.class.getName());

    private DefaultComponentRepository repository;
    private ComponentLoader            loader;
    private ComponentContext           context;

    public DefaultLaunchEnvironment() {
        String home = System.getProperty("app.home");
        if (!StringUtils.hasText(home)) {
            home = System.getProperty("user.dir");
            System.setProperty("app.home", home);
            logger.debug("app.home is not set, use user.dir as app.home: {}", home);
        }
        home = FilenameUtils.normalize(System.getProperty("app.home"));
        //noinspection ConstantConditions
        System.setProperty("app.home", home);

        String appHost = System.getProperty("app.host");
        if (!StringUtils.hasText(appHost)) {
            List<String> addresses = getLocalAddresses();
            if (addresses.isEmpty()) {
                throw new ApplicationContextException("You server isn't configure any address");
            }
            System.setProperty("app.host", addresses.get(0));
            logger.debug("app.host is not set, use local ip as default");
        }
        repository = createComponentRepository(home);
        try {
            repository.start();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Can't start the default component repository, " +
                                                    "because of: " + e.getMessage(), e);
        }
    }

    /**
     * 根据Main Resource的元信息，创建相应的Launcher对象
     *
     * @param component 主启动组件
     * @return App Launcher对象
     */
    @Override
    public AppLauncher createLauncher(Component component) throws IOException {
        AppLauncher launcher = new AppLauncher(component, this);
        //配置 class world
        this.loader = createLoader(component);
        this.context = (ComponentContext) this.loader;
        return launcher;
    }

    @Override
    public void execute(AppLauncher launcher, String[] args) throws Exception {
        List<String> list = new ArrayList<String>(args.length);
        Collections.addAll(list, args);
        String host = null;
        int index = 0;
        int mode, port = 0;
        if (list.contains("--host")) {
            host = list.get(list.indexOf("--host") + 1);
        }
        if (list.contains("--index")) {
            index = Integer.valueOf(list.get(list.indexOf("--index") + 1));
        }
        if (list.contains("--port")) {
            port = Integer.parseInt(list.get(list.indexOf("--port") + 1));
        }
        if (list.contains("--stop")) {
            mode = MODE_STOP;
            if (host == null) host = "localhost";
        } else {
            mode = MODE_START;
            host = null;
        }
        Executable executable = configure(launcher, host, index, port);
        if (mode == MODE_START) {
            executable.start();
        } else {
            executable.exit();
        }
    }

    protected Executable configure(AppLauncher launcher, String host, int index, int port) {
        //设定了通过本地端口与已经启动的进程通讯
        if (host != null) {
            if (port > 0) {
                return new LauncherThroughPort(port);
            }else{
                return new LauncherThroughFile(index);
            }
        }// itself
        return launcher;
    }


    // ------------------------------------------------------------
    //     对 ComponentRepository 代理接口的实现
    // ------------------------------------------------------------

    @Override
    public Component resolveComponent(String strDependency)
            throws DependencyNotMeetException, InvalidComponentNameException {
        return repository.resolveComponent(strDependency);
    }


    /**
     * 自顶向下的加载组件
     *
     * @param component 被加载的组件
     */
    public void load(Component component) throws Exception {
        long start = System.currentTimeMillis();
/*
        Set<Component> dependedComponents = component.getAllDependedComponents();
        ArrayList<Component> depends = new ArrayList<Component>(dependedComponents);
        repository.sortComponents(depends);
        if( logger.isDebugEnabled() ){
            logger.debug("Loading components by: \n\t {}",
                        org.apache.commons.lang.StringUtils.join(depends, "\n\t"));
        }
        //把自己放在最后面
        depends.add(component);
        //已经按照依赖排序了
        for (Component depend : depends) {
            loader.quickLoad(depend);
        }
*/
        loader.load(component);
        registerWorldAndComponents(component);

        banner("Container starts took {}", formatDurationHMS(System.currentTimeMillis() - start));

        publishContainerStartedEvent();
    }

    void registerWorldAndComponents(Component component) {
        ApplicationContext mainContext = component.getApplication();
        if (mainContext == null) return;
        MBeanExporter exporter;
        try {
            exporter = mainContext.getBean(MBeanExporter.class);
        } catch (BeansException e) {
            logger.warn("You should configure a MBeanExporter in main app context");
            return;
        }
        List<Component> components = repository.getComponents();
        for (Component comp : components) {
            if (repository.isApplication(comp.getGroupId())) {
                exporter.registerManagedResource(comp, ((DefaultComponent) comp).getObjectName());
            }
        }
    }

    public void unload(Component component) {
        publishContainerStoppingEvent();
        long start = System.currentTimeMillis();
        loader.unload(component);
        banner("Container stops took {}", formatDurationHMS(System.currentTimeMillis() - start));
    }

    public List<ApplicationContext> getApplications() {
        return context.getApplicationFeatures();
    }

    void publishContainerStartedEvent() {
        logger.debug("The component container is started");
        ContainerStartedEvent event = new ContainerStartedEvent(this);
        publishEvent(event);
    }

    void publishContainerStoppingEvent() {
        logger.debug("The component container is stopping");
        ContainerEvent event = new ContainerStoppingEvent(this);
        publishEvent(event);
    }

    void publishEvent(ApplicationEvent event) {
        List<ApplicationContext> applications = getApplications();
        ContextUtils.publishEvent(applications, event);
    }


    @Override
    public void shutdown() {
        banner("Shutting down...");
        if (repository != null) {
            repository.stop();
        }
    }

    protected DefaultComponentRepository createComponentRepository(String home) {
        return new DefaultComponentRepository(home);
    }

    protected ComponentLoader createLoader(Component component) {
        DefaultComponentLoader loader = new DefaultComponentLoader(repository);
        String featureResolvers = System.getProperty("component.feature.resolvers");
        if (StringUtils.hasText(featureResolvers)) {
            for (String resolverFqn : featureResolvers.split(",")) {
                try {
                    logger.debug("Found extended feature resolver: " + resolverFqn);
                    Class resolverClass = Class.forName(resolverFqn, true, component.getClassLoader());
                    FeatureResolver resolver = (FeatureResolver) resolverClass.newInstance();
                    loader.registerResolver(resolver);
                } catch (Exception ex) {
                    logger.error("Can't instantiate the feature resolver: " + resolverFqn, ex);
                }
            }
        }
        return loader;
    }

    void banner(String message, Object... args) {
        logger.info(LogUtils.banner(message, args));
    }

    public static List<String> getLocalAddresses() {
        List<IndexAndIp> localAddresses = new ArrayList<IndexAndIp>(2);
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                Enumeration<InetAddress> addresses = nic.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    String hostAddress = address.getHostAddress();
                    if (hostAddress.contains(":")) continue; // ipv6
                    if ("127.0.0.1".equals(hostAddress)) continue;
                    localAddresses.add(new IndexAndIp(nic.getIndex(), hostAddress));
                }
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException("Can't find local address", e);
        }
        //按照网卡接口号先后排序
        Collections.sort(localAddresses);
        List<String> addresses = new ArrayList<String>();
        for (IndexAndIp indexAndIp : localAddresses) {
            addresses.add(indexAndIp.ip);
        }
        return addresses;
    }

    static class IndexAndIp implements Comparable<IndexAndIp> {
        private int    index;
        private String ip;

        public IndexAndIp(int index, String ip) {
            this.index = index;
            this.ip = ip;
        }

        @Override

        public int compareTo(@SuppressWarnings("NullableProblems") IndexAndIp another) {
            return this.index - another.index;
        }
    }
}
