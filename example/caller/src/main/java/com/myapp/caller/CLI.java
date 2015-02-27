/**
 * @author XiongJie, Date: 13-12-3
 */
package com.myapp.caller;

import com.myapp.api.RouteAPI;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/** Accept test caller */
public class CLI {

    /**
     * java -Drouter.port=1097 -Drouter.address=localhost -jar path/to/com.myapp.caller-1.0.0.jar jobId
     *
     * @param args jobId(mandatory)
     */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You must specify a job id");
        String jobId = args[0];
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(RouteAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/router",
                                                System.getProperty("router.address", "localhost"),
                                                System.getProperty("router.port", "1097")));
        factoryBean.afterPropertiesSet();
        RouteAPI router = (RouteAPI) factoryBean.getObject();
        Object result = router.perform(jobId);
        System.out.println("Got router response: " + result);
    }
}
