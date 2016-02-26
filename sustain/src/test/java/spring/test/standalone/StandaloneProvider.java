/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.standalone;

import org.springframework.stereotype.Component;
import spring.test.api.ServiceProvider;

/** 独立的服务提供者 */
@Component
public class StandaloneProvider implements ServiceProvider {
    @Override
    public String provide(String userName) {
        return "[ " + userName + " ] message by standalone";
    }
}
