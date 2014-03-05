/**
 * @author XiongJie, Date: 13-11-4
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.container.Executable;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/** 通过一个本地通讯机制，让某个已经存在的进程(AppLauncher)执行相应的命令 */
public class LauncherThroughPort implements Executable {
    private final String      host;
    private final int         port;
    private       Executable remote;

    public LauncherThroughPort(int port) {
        this("localhost", port);
    }

    public LauncherThroughPort(String host, int port) {
        this.host = host;
        this.port = port;
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(Executable.class);
        String serviceUrl = String.format("rmi://%s:%d/%sLauncher",
                                          this.host, this.port, System.getProperty("app.name"));
        factoryBean.setServiceUrl(serviceUrl);
        try {
            factoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't lookup Executable Service at: " + serviceUrl);
        }
        remote = (Executable) factoryBean.getObject();
    }

    @Override
    public void start() throws Exception {
        remote.start();
    }

    @Override
    public void exit() {
        try {
            remote.exit();
        } catch (Exception e) {
            System.out.println("Can't exit remote program, because of:" + e.getMessage());
        }
    }

    @Override
    public void reload() {
        remote.reload();
    }

    @Override
    public String toString() {
        return "AppLauncher{" + host + ":" + port + '}';
    }
}
