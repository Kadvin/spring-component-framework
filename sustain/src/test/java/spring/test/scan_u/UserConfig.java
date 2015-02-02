/**
 * Developer: Kadvin Date: 15/2/2 上午11:08
 */
package spring.test.scan_u;

import net.happyonroad.component.container.ServiceImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.test.scan_p.Provider;

/**
 * <h1>Component7的App Config </h1>
 *
 * 之所以命名为 UserConfig，是为了能够被生成到 最终的jar中间去(必须以User开头)
 */
@Configuration
public class UserConfig {
    @Autowired
    ServiceImporter importer;

    @Bean
    public Provider serviceProvider(){
        return importer.imports(Provider.class);
    }
}
