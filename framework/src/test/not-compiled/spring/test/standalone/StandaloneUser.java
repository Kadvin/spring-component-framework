/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.standalone;

import spring.test.api.ServiceProvider;
import spring.test.api.ServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 独立app里面使用的类 */
@Component
public class StandaloneUser implements ServiceUser {
    @Autowired
    private ServiceProvider provider;

    @Override
    public String work() {
        String msg = provider.provide("StandaloneUser");
        System.out.println(msg);
        return msg;
    }
}
