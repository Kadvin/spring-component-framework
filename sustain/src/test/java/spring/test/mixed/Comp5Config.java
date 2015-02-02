/**
 * Developer: Kadvin Date: 15/2/2 上午11:08
 */
package spring.test.mixed;

import net.happyonroad.component.container.ServiceExporter;
import net.happyonroad.component.container.ServiceImporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.test.api.ServiceProvider;
import spring.test.api.ServiceUser;

/**
 * Component5的App Config
 */
@Configuration
public class Comp5Config implements InitializingBean {
    @Autowired
    ServiceExporter exporter;
    @Autowired
    ServiceImporter importer;

    @Autowired
    ServiceProvider mixedServiceProvider;
    @Autowired
    ServiceUser mixedServiceUser;

    @Bean
    public ServiceProvider serviceProvider() {
        return importer.imports(ServiceProvider.class, "test");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        exporter.exports(ServiceProvider.class, mixedServiceProvider, "mixed");
        exporter.exports(ServiceUser.class, mixedServiceUser, "mixed");
    }
}
