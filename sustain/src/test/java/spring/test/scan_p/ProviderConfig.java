/**
 * Developer: Kadvin Date: 15/2/2 上午11:00
 */
package spring.test.scan_p;

import net.happyonroad.spring.service.ServiceExporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * <h1>Component6的App Config</h1>
 *
 * 之所以命名为 ProviderConfig，是为了能够被生成到 最终的jar中间去(必须以Provider开头)
 */
@Configuration
public class ProviderConfig implements InitializingBean{
    @Autowired
    ServiceExporter exporter;

    @Autowired
    private Provider provider;

    @Override
    public void afterPropertiesSet() throws Exception {
        exporter.exports(Provider.class, provider);
    }
}
