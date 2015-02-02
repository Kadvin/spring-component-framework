/**
 * Developer: Kadvin Date: 15/2/2 上午11:00
 */
package spring.test.provider;

import net.happyonroad.component.container.ServiceExporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import spring.test.api.ServiceProvider;

/**
 * Component3的App Config
 */
@Configuration
public class Comp3Config implements InitializingBean{
    @Autowired
    ServiceExporter exporter;

    @Autowired
    private ServiceProvider serviceProvider;

    @Override
    public void afterPropertiesSet() throws Exception {
        exporter.exports(ServiceProvider.class, serviceProvider, "test");
    }
}
