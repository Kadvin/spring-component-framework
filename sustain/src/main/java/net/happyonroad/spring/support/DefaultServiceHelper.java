/**
 * Developer: Kadvin Date: 15/2/2 上午11:31
 */
package net.happyonroad.spring.support;

import net.happyonroad.spring.exception.ServiceConfigurationException;
import net.happyonroad.spring.service.MutableServiceRegistry;
import net.happyonroad.spring.service.ServiceExporter;
import net.happyonroad.spring.service.ServiceImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Modifier;

/**
 * <h1>导入/导出服务的辅助工具</h1>
 */
public class DefaultServiceHelper implements ServiceImporter, ServiceExporter{
    private MutableServiceRegistry registry;
    Logger logger = LoggerFactory.getLogger(getClass());

    public DefaultServiceHelper(MutableServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <T> void exports(Class<T> serviceClass, T service, String hint) {
        if( !Modifier.isAbstract(serviceClass.getModifiers()) ){
            logger.warn("Import a concrete service: " + serviceClass.getName());
        }
        this.registry.register(serviceClass, service, hint);
    }

    @Override
    public <T> void exports(Class<T> serviceClass, T service) {
        if( !Modifier.isAbstract(serviceClass.getModifiers()) ){
            logger.warn("Import a concrete service: " + serviceClass.getName());
        }
        this.registry.register(serviceClass, service);
    }

    @Override
    public <T> T imports(Class<T> serviceClass) throws ServiceConfigurationException {
        return imports(serviceClass, "*");
    }

    @Override
    public <T> T imports(Class<T> serviceClass, String hint) throws ServiceConfigurationException {
        T service = registry.getService(serviceClass, hint);
        if(service != null){
            //仅以接口的形式暴露时需要进行proxy
            if(serviceClass.isInterface()){
                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setTarget(service);
                proxyFactory.setInterfaces(serviceClass);
                proxyFactory.setOpaque(true);
                Object proxy;
                ClassLoader classLoader = service.getClass().getClassLoader();
                try {
                    proxy = proxyFactory.getProxy(classLoader);
                } catch (IllegalArgumentException e) {
                    throw new ServiceConfigurationException("Can't create service proxy," +
                                                            " current class loader is " + classLoader, e);
                }
                //noinspection unchecked
                return (T) proxy;
            }else{
                // TODO 直接将服务实例暴露可能会有问题，就是在多个context里面存在同一个实例
                // 如果该实例实现了某种其他接口，这个接口将会影响整个系统，例如：
                // 该实例额外实现了 ApplicationListener接口
                //   这可能会导致其他模块向其发出多次事件（我现在已经在 smart application event multicaster里面避免了这个情况)
                // 如果该实例额外实现了某种其他类也会暴露的服务接口
                //   这会导致意外的服务暴露和接口冲突
                return service;
            }
        }else{
            throw new ServiceConfigurationException("Can't find " + serviceClass + " with hint " + hint);
        }
    }

}
