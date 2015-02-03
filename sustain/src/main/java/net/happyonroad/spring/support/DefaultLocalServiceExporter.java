/**
 * Developer: Kadvin Date: 15/2/3 上午9:51
 */
package net.happyonroad.spring.support;

import net.happyonroad.spring.service.LocalServiceExporter;
import net.happyonroad.spring.service.ServiceExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * <h1>基于特定Application Context的Service Exporter</h1>
 */
public class DefaultLocalServiceExporter implements LocalServiceExporter{
    @Autowired
    ServiceExporter exporter;
    @Autowired
    ApplicationContext provider;

    public DefaultLocalServiceExporter(ServiceExporter exporter, ApplicationContext applicationContext) {
        this.exporter = exporter;
        this.provider = applicationContext;
    }

    @Override
    public <T> void exports(Class<T> serviceClass, String hint) {
        T service = provider.getBean(serviceClass);
        exporter.exports(serviceClass, service, hint);
    }

    @Override
    public <T> void exports(Class<T> serviceClass) {
        exports(serviceClass, "default");
    }
}
