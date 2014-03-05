/**
 * @author XiongJie, Date: 13-12-3
 */
package com.myapp.caller;

import com.myapp.api.ServerAPI;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/** Accept test caller */
public class CLI {

    /**
     * java -Dserver.port=1097 -Dserver.address=localhost -jar path/to/com.myapp.caller-1.0.0.jar jobId
     *
     * @param args jobId(mandatory)
     */
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You must specify a job id");
        String jobId = args[0];
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(ServerAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/server",
                                                System.getProperty("server.address", "localhost"),
                                                System.getProperty("server.port", "1097")));
        factoryBean.afterPropertiesSet();
        ServerAPI server = (ServerAPI) factoryBean.getObject();
        Object result = server.perform(jobId);
        System.out.println("Got server response: " + result);
    }
}
