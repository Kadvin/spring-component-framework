/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spring.test.api.ServiceProvider;
import spring.test.api.ServiceUser;

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
