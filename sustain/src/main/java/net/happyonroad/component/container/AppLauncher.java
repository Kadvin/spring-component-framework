/**
 * @author XiongJie, Date: 13-8-28
 */
package net.happyonroad.component.container;

import net.happyonroad.component.classworld.MainClassLoader;
import net.happyonroad.component.container.support.DefaultLaunchEnvironment;
import net.happyonroad.component.container.support.ShutdownHook;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentException;
import net.happyonroad.component.core.support.ComponentURLStreamHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;

import static net.happyonroad.util.LogUtils.banner;
import static org.apache.commons.lang.time.DurationFormatUtils.formatDurationHMS;

/**
 * <h2>扩展了Maven Plexus的Launcher</h2>
 * <ul>
 * <li>简化uberjar规范</li>
 * <li>解析被启动的目标组件的Dependencies信息为Classworld</li>
 * <li>构建合适的Spring Context</li>
 * <li>与其他组件交互</li>
 * </ul>
 * <p>对某个组件进行
 * <ol><li>加载<li>启动<li>停止<li>卸载</ol>
 * </p>
 */
public class AppLauncher implements Executable {
    private static       Logger logger     = LoggerFactory.getLogger(AppLauncher.class.getName());
    private static final String CMD_EXIT   = "exit";
    private static final String CMD_RELOAD = "reload";

    private boolean running;

    protected LaunchEnvironment environment;
    protected Component         mainComponent;
    //protected PomClassWorld     pomWorld;
    private   boolean           exiting;

    public AppLauncher(Component mainComponent, LaunchEnvironment environment) {
        super();
        if (mainComponent == null) {
            throw new IllegalArgumentException("The main component must been specified!");
        }
        this.mainComponent = mainComponent;
        //String theMainClassName = this.mainComponent.getManifestAttribute("Main-Class");
        //setAppMain(theMainClassName, this.mainComponent.getId());
        this.environment = environment;
        //super.world = this.pomWorld = new PomClassWorld();
        //this.pomWorld.setMainComponentId(this.mainComponent.getId());
    }

    /**
     * 启动当前的组件Jar包
     *
     */
    public void start() throws Exception {
        try {
            long start = System.currentTimeMillis();
            logger.info(banner("Loading components starts from {}", this.mainComponent));
            environment.load(this.mainComponent);
            logger.info(banner("Loaded  components starts from {}", this.mainComponent));
            exportAsRMI();
            addShutdownHook();
            logger.info(banner("The {} is started", getAppName()));
            logger.info(banner("System starts took {}", formatDurationHMS(System.currentTimeMillis() - start)));
            //让主线程基于STDIO接受交互命令
            //以后应该让CLI组件托管这块工作
            processCommands();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /** 退出 */
    public void exit() {
        if( exiting ){
            logger.debug("Another thread is shutting down");
            return;
        }
        final long start = System.currentTimeMillis();
        //noinspection finally
        try {
            logger.info(banner("Unloading the main component {}", this.mainComponent));
            exiting = true;
            environment.unload(this.mainComponent);
            logger.info(banner("Unloaded  the main component {}", this.mainComponent));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            running = false;
            //启动一个额外的线程停止自身
            new Thread("Stopper") {
                @Override
                public void run() {
                    logger.info(banner("System stops took {}", formatDurationHMS(System.currentTimeMillis() - start)));
                    System.exit(0);
                }
            }.start();
        }
    }

    @Override
    public boolean exiting() {
        return exiting;
    }

    /*重新加载*/
    public void reload() {

    }

    // ------------------------------------------------------------
    //     辅助管理
    // ------------------------------------------------------------

    /**
     * 将本对象通过Spring的RMI机制暴露出去
     *
     * @throws Exception
     */
    private void exportAsRMI() throws Exception {
        String appHost = System.getProperty("app.host");
        String appPort = System.getProperty("app.port");
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(Executable.class);
        String serviceName = getAppName() + "Launcher";
        //exporter.setRegistryHost(appHost);
        exporter.setRegistryPort(Integer.valueOf(appPort));
        exporter.setServiceName(serviceName);
        exporter.setService(this);
        exporter.afterPropertiesSet();

        String serviceUrl = String.format("rmi://%s:%s/%s", appHost, appPort, serviceName);
        logger.info(banner("Export Executable Service at {}", serviceUrl));
    }

    private void addShutdownHook() {
       Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }

    /** 处理当前进程的命令行交互 */
    private void processCommands() {
        running = true;
        boolean normal = true;
        while (running) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                if (normal) System.out.println("Input command:");
                String command = reader.readLine();
                command = command.trim();
                if (StringUtils.isEmpty(command)) continue;
                logger.info("Get command: `{}` ", command);
                if (CMD_EXIT.equalsIgnoreCase(command)) {
                    exit();
                } else if (CMD_RELOAD.equalsIgnoreCase(command)) {
                    reload();
                } else {
                    process(command);
                }
                normal = true;
                Thread.sleep(1000);
            } catch (Exception e) {
                //We will catch exception while started by wrapper process
                //Thread.yield();
                try {
                    //if we only yield without sleep,
                    // the CPU usage will be raised to 100%, but other application can works
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    //skip it
                }
                normal = false;
            }
        }
    }

    protected String getAppName() {
        return System.getProperty("app.name", "Unknown");
    }

    /**
     * Process user specified command
     *
     * @param command the command
     */
    protected void process(String command) {
        try {
            Method method = this.getClass().getMethod(command);
            logger.info("Try to delegate '{}' to launcher directly.", command);
            method.invoke(this);
            logger.info("Invoke '{}' to launcher directly successfully. \r\n", command);
        } catch (NoSuchMethodException e) {
            logger.warn("unrecognized command: '{}'", command);
        } catch (Exception e) {
            logger.warn("Failed to execute: '{}'", command);
        }
    }

    // ------------------------------------------------------------
    //     入口静态方法
    // ------------------------------------------------------------

    /**
     * <h1>支持系统启动与停止</h1>
     * <p/>
     * <h2>启动方式</h2>
     * 开发模式下，通过命令行以：
     * <p><strong>java -classpath jdk/jars;container/classes;other/classes
     * dnt.component.container.AppLauncher depended.group.artifact-version</strong></p>
     * 生产模式下，通过命令行以：
     * <p><strong>java -Dapp.home=path/to/home -jar path/to/container.jar depended.group.artifact-version</strong></p>
     * 的方式启动
     * <p>
     * 定制方式：
     * 用户可以通过　-Dapp.launch.environment=the.launch.env.class 的方式定制启动逻辑
     * 但请注意将相应的Launch Env类配置在主jar的class-path中（或开发环境的pom依赖下）
     * </p>
     * 退出情况:
     * <ul>
     * <li>0: 正常退出</li>
     * <li>1: Launch Env不能初始化</li>
     * <li>2: 目标主jar|classes未定义</li>
     * <li>3: 目标主jar|classes无法初始化</li>
     * <li>100: 未知异常</li>
     * <li>其他：业务异常，待明确</li>
     * </ul>
     * <p/>
     * <h2>停止方式</h2>
     * 开发模式下，通过命令行以：
     * <p><strong>java -classpath jdk/jars;container/classes;other/classes
     * dnt.component.container.AppLauncher depended.group.artifact-version --stop</strong></p>
     * 生产模式下，通过命令行以：
     * <p><strong>java -Dapp.home=path/to/home -jar path/to/container.jar depended.group.artifact-version --stop</strong></p>
     * 的方式停止
     *
     * @param args 最少要指定一个入口jar文件或者入口主模块输出路径
     */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You must specify a main dependency");
        //设定的路径可能包括lib/等目录
        String[] strings = args[0].split("\\|/");
        if (strings.length > 1) {
            args[0] = strings[strings.length - 1];
        }
        try {
            // When started by Service wrapper, the context class loader is URLClassLoader of wrapper.jar
            //  but this main class is loaded by the framework jar's ClassLoader(FactoryClassLoader)
            Thread.currentThread().setContextClassLoader(MainClassLoader.getInstance());
            // To register the url handler by current context class loader, instead of system bootstrap class loader
            URL.setURLStreamHandlerFactory(ComponentURLStreamHandlerFactory.getFactory());
            int exitCode = mainWithExitCode(args);
            System.exit(exitCode);
        } catch (LaunchException e) {
            System.err.println(e.getMessage());
            System.exit(e.getExitCode());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }
    }

    /**
     * 启动逻辑，暂时还没有支持停止
     *
     * @param args The application command-line arguments.
     * @return an integer exit code
     * @throws Exception If an error occurs.
     */
    public static int mainWithExitCode(String[] args) throws Exception {
        LaunchEnvironment environment = detectEnvironment();

        Component mainComponent = null;
        try {
            logger.debug(banner("Resolving starts from {}", args[0]));
            mainComponent = environment.resolveComponent(args[0]);
            logger.debug(banner("Resolved  starts from {}", args[0]));

            AppLauncher launcher = environment.createLauncher(mainComponent);
            //去掉主入口参数之后，将其余参数传入
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            //启动程序，根据剩余的 newArgs，可能有多种启动方式
            // 包括: 打印组件依赖列表
            //      启动系统(在当前进程内启动系统)
            //      停止系统(通过本机Socket停止另外一个已经在运行的系统)
            environment.execute(launcher, newArgs);
            return 0;
        }catch (ComponentException ex){
            logger.error("{} : {}", ex.getPath(), ex.getRootCause().getMessage());
            return -1;
        }catch (Throwable ex){
            logger.error("Failed: " + ex.getMessage(), ex);
            return -1;
        } finally {
            if (mainComponent != null) {
                environment.unload(mainComponent);
            }
            environment.shutdown();
        }
    }

    protected static LaunchEnvironment detectEnvironment() throws Exception {
        String env = System.getProperty("app.launch.environment");
        if (env != null) {
            return (LaunchEnvironment) Class.forName(env).newInstance();
        } else {
            return new DefaultLaunchEnvironment();
        }

    }
}
