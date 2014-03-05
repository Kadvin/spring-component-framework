/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.user;

import spring.test.api.ServiceProvider;
import spring.test.api.ServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 测试的服务用户 */
@Component
public class TestServiceUser implements ServiceUser {
    @Autowired
    ServiceProvider provider;

    @Override
    public String work() {
        String msg = provider.provide("TestServiceUser");
        System.out.println(msg);
        return msg;
    }
}
