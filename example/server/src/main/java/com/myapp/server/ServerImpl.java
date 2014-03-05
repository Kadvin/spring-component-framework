/**
 * @author XiongJie, Date: 13-11-27
 */
package com.myapp.server;

import com.myapp.api.CacheService;
import com.myapp.api.ClientAPI;
import com.myapp.api.ServerAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.util.*;

@org.springframework.stereotype.Component
public class ServerImpl implements ServerAPI {
    @Autowired
    private CacheService cacheService;

    private Map<String, ClientAPI> clients = new HashMap<String, ClientAPI>();

    public String register(String clientId, String address) {
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(ClientAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/client",
                                                address,
                                                System.getProperty("client.app.port","1096")));
        factoryBean.afterPropertiesSet();
        ClientAPI client = (ClientAPI) factoryBean.getObject();
        String token = UUID.randomUUID().toString();
        clients.put(token, client);
        System.out.println(String.format("A client(%s) at %s registered, and assigned with token(%s)",
                                         clientId, address, token));
        return token;
    }

    public Object perform(String job){
        // Reused cached result first
        Object result = cacheService.pick(job);
        if( result != null )
        {
            System.out.println(String.format("Return cached job(%s) with effort %s", job, result));
            return result;
        }
        // pick a client to perform the job if no cached result
        ClientAPI client = pickClient();
        if( client == null )
            throw new IllegalStateException("There is no client available to perform the job: " + job);
        result = client.perform(job);
        // store the result to reused latter
        cacheService.store(job, result);
        System.out.println(String.format("Client perform job(%s) with effort %s", job, result));
        return result;
    }

    private ClientAPI pickClient() {
        int max = clients.size();
        int randIndex = new Random().nextInt(max);
        return (ClientAPI) clients.values().toArray()[randIndex];
    }
}
