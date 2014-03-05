/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.standalone;

import spring.test.api.ServiceProvider;
import org.springframework.stereotype.Component;

/** 独立的服务提供者 */
@Component
public class StandaloneProvider implements ServiceProvider {
    @Override
    public String provide(String userName) {
        return "[ " + userName + " ] message by standalone";
    }
}
