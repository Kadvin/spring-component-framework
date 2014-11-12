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
import net.happyonroad.util.LogUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** 为了让Launch静态程序可以被定制化，从原 Plexus/Launch中分离出来的对象 */
public class DefaultLaunchEnvironment implements LaunchEnvironment {
    public static final int MODE_START  = 1;
    public static final int MODE_STOP   = -1;
    public static final int MODE_RELOAD = 0;

    private Logger logger = LoggerFactory.getLogger(DefaultLaunchEnvironment.class.getName());

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
        System.setProperty("app.home", home);

        String appHost = System.getProperty("app.host");
        if (!StringUtils.hasText(appHost)) {
            appHost = "localhost";
            System.setProperty("app.host", appHost);
            logger.debug("app.host is not set, use localhost as default");
        }
        String appPort = System.getProperty("app.port");
        if (!StringUtils.hasText(appPort)) {
            appPort = "1099";
            System.setProperty("app.port", appPort);
            logger.debug("app.port is not set, use 1099 as default");
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

    /**
     * 在本环境下启动应用程序
     *
     * @param launcher 启动器
     * @throws Exception 错误
     */
    /*package*/ void start(Executable launcher) throws Exception {
        launcher.start();
    }

    /**
     * 停止一个远程的launcher对象
     *
     * @param launcher 远程launcher对象
     */
    /*package*/ void stop(Executable launcher) {
        launcher.exit();
    }

    /**
     * 要求远程launcher对象刷新
     *
     * @param launcher 远程launcher对象
     */
    /*package*/ void reload(Executable launcher) {
        launcher.reload();
    }

    @Override
    public void execute(AppLauncher launcher, String[] args) throws Exception {
        int mode;
        List<String> list = new ArrayList<String>(args.length);
        Collections.addAll(list, args);
        String host = null;
        int port = 0;
        if (list.contains("--host")) {
            host = list.get(list.indexOf("--host") + 1);
        }
        if (list.contains("--port")) {
            port = Integer.parseInt(list.get(list.indexOf("--port") + 1));
        }
        if (list.contains("--stop")) {
            mode = MODE_STOP;
            if(host == null)
                host = "localhost";
            if(port == 0)
                port = Integer.valueOf(System.getProperty("app.port", "1099"));
        } else if (list.contains("--reload")) {
            mode = MODE_RELOAD;
            if(host == null)
                host = "localhost";
            if(port == 0)
                port = Integer.valueOf(System.getProperty("app.port", "1099"));
        } else {
            mode = MODE_START;
            host = null;
            port = 0;
        }
        Executable executable = configure(launcher, host, port);
        switch (mode) {
            case MODE_START:
                start(executable);
                break;
            case MODE_STOP:
                stop(executable);
                break;
            case MODE_RELOAD:
                reload(executable);
                break;
            default:
                start(executable);
        }
    }

    protected Executable configure(AppLauncher launcher, String host, int port) {
        //设定了通过本地端口与已经启动的进程通讯
        if (host != null) {
            return new LauncherThroughPort(host, port);
        } else if (port > 0) {
            return new LauncherThroughPort(port);
        }
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
        loader.load(component);

        registerWorldAndComponents(component);

        publishContainerStartedEvent();
    }

    void registerWorldAndComponents(Component component) {
        ApplicationContext mainContext = component.getApplication();
        if(mainContext == null) return;
        MBeanExporter exporter;
        try {
            exporter = mainContext.getBean(MBeanExporter.class);
        } catch (BeansException e) {
            logger.warn("You should configure a MBeanExporter in main app context");
            return;
        }
        Set<Component> components =repository.getComponents();
        for (Component comp : components) {
            if( repository.isApplication(comp.getGroupId())){
                exporter.registerManagedResource(comp, ((DefaultComponent) comp).getObjectName());
            }
        }
    }

    public void unload(Component component) {
        publishContainerStoppingEvent();

        loader.unload(component);
    }

    void publishContainerStartedEvent() {
        banner("The component container is started");
        List<ApplicationContext> applications = context.getApplicationFeatures();
        ContainerStartedEvent event = new ContainerStartedEvent(this);
        for (ApplicationContext application : applications) {
            application.publishEvent(event);
        }
    }

    void publishContainerStoppingEvent() {
        banner("The component container is stopping");
        List<ApplicationContext> applications = context.getApplicationFeatures();
        Collections.reverse(applications);

        ContainerEvent event = new ContainerStoppingEvent(this);
        for (ApplicationContext application : applications) {
            application.publishEvent(event);
        }
    }


    @Override
    public void shutdown() {
        banner("Shutting down");
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
        if(StringUtils.hasText(featureResolvers)){
            for(String resolverFqn: featureResolvers.split(",")){
                try{
                    banner("Found extended feature resolver: " + resolverFqn);
                    Class resolverClass = Class.forName(resolverFqn, true, component.getClassLoader());
                    FeatureResolver resolver = (FeatureResolver) resolverClass.newInstance();
                    loader.registerResolver(resolver);
                }catch (Exception ex){
                    logger.error("Can't instantiate the feature resolver: " + resolverFqn, ex);
                }
            }
        }
        return loader;
    }

    void banner(String message){
        logger.info(LogUtils.banner(message));
    }

}
