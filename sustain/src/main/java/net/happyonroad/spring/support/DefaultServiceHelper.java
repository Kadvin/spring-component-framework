/**
 * Developer: Kadvin Date: 15/2/2 上午11:31
 */
package net.happyonroad.spring.support;

import net.happyonroad.spring.service.MutableServiceRegistry;
import net.happyonroad.spring.service.ServiceExporter;
import net.happyonroad.spring.service.ServiceImporter;
import net.happyonroad.spring.exception.ServiceConfigurationException;
import org.springframework.aop.framework.ProxyFactory;

/**
 * <h1>导入/导出服务的辅助工具</h1>
 */
public class DefaultServiceHelper implements ServiceImporter, ServiceExporter{
    private MutableServiceRegistry registry;

    public DefaultServiceHelper(MutableServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <T> void exports(Class<T> serviceClass, T service, String hint) {
        this.registry.register(serviceClass, service, hint);
    }

    @Override
    public <T> void exports(Class<T> serviceClass, T service) {
        this.registry.register(serviceClass, service);
    }

    @Override
    public <T> T imports(Class<T> serviceClass) throws ServiceConfigurationException {
        return imports(serviceClass, "default");
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
                return service;
            }
        }else{
            throw new ServiceConfigurationException("Can't find " + serviceClass + " with hint " + hint);
        }
    }

}
