/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.provider;

import spring.test.api.ServiceProvider;
import org.springframework.stereotype.Component;

/** 测试的服务提供者 */
@Component
public class TestServiceProvider implements ServiceProvider {
    @Override
    public String provide(String userName) {
        return "[ " + userName + " ] message by test";
    }
}
