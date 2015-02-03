/**
 * Developer: Kadvin Date: 15/2/3 上午9:54
 */
package net.happyonroad.spring.config;

import net.happyonroad.spring.service.LocalServiceExporter;
import net.happyonroad.spring.service.ServiceExporter;
import net.happyonroad.spring.support.DefaultLocalServiceExporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * <h1>抽象的Application Configuration</h1>
 * 主要功能是：
 * <p/>
 * <ul>
 * <li>注入 Application Context</li>
 * <li>注入 Service Export</li>
 * <li>提供简便的 exports 方法</li>
 * <li>构造 Local Service Exporter</li>
 * <li>提供简便的 本地 exports 方法（无需 bean reference)</li>
 * </ul>
 */
public abstract class AbstractAppConfig extends AbstractUserConfig implements InitializingBean {
    @Autowired
    protected ServiceExporter exporter;
    @Autowired
    protected ApplicationContext applicationContext;

    @Bean
    LocalServiceExporter localExporter() {
        return new DefaultLocalServiceExporter(exporter, applicationContext);
    }

    // 简单封装 exporter

    /**
     * <h2>暴露某个服务</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param service      实际的服务实例（不能为空)
     * @param hint         服务的注释
     * @param <T>          服务的类型
     */
    protected <T> void exports(Class<T> serviceClass, T service, String hint) {
        exporter.exports(serviceClass, service, hint);
    }

    /**
     * <h2>暴露某个服务(hint = default)</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param service      实际的服务实例（不能为空)
     * @param <T>          服务的类型
     */
    protected <T> void exports(Class<T> serviceClass, T service) {
        exporter.exports(serviceClass, service);
    }

    // 封装 local exporter

    /**
     * <h2>暴露某个服务</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param hint         服务的注释
     * @param <T>          服务的类型
     */
    protected <T> void exports(Class<T> serviceClass, String hint) {
        localExporter().exports(serviceClass, hint);
    }

    /**
     * <h2>暴露某个服务(hint = default)</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param <T>          服务的类型
     */
    protected <T> void exports(Class<T> serviceClass) {
        localExporter().exports(serviceClass);
    }

    @Override
    public final void afterPropertiesSet() throws Exception {
        beforeExports();
        try {
            doExports();
        } finally {
            afterExports();
        }
    }

    /**
     * 执行服务导出的之前工作
     */
    protected void beforeExports() {}

    /**
     * 执行服务导出的工作
     */
    protected void doExports() {}

    /**
     * 执行服务导出的之后工作
     */
    protected void afterExports() {}
}
