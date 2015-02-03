/**
 * Developer: Kadvin Date: 15/2/2 上午11:08
 */
package spring.test.user;

import net.happyonroad.spring.service.ServiceImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.test.api.ServiceProvider;

/**
 * Component4的App Config
 */
@Configuration
public class Comp4Config {
    @Autowired
    ServiceImporter importer;

    @Bean
    public ServiceProvider serviceProvider(){
        return importer.imports(ServiceProvider.class, "test");
    }
}
