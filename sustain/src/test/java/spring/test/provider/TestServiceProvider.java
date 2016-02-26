/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.provider;

import org.springframework.stereotype.Component;
import spring.test.api.ServiceProvider;

/** 测试的服务提供者 */
@Component
public class TestServiceProvider implements ServiceProvider {
    @Override
    public String provide(String userName) {
        return "[ " + userName + " ] message by test";
    }
}
