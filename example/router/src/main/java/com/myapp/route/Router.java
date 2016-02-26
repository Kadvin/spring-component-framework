/**
 * @author XiongJie, Date: 13-11-27
 */
package com.myapp.route;

import com.myapp.api.CacheService;
import com.myapp.api.RouteAPI;
import com.myapp.api.WorkAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@org.springframework.stereotype.Component
public class Router implements RouteAPI {
    @Autowired
    private CacheService cacheService;

    @Value("${worker.port}")
    int workerPort;

    private Map<String, WorkAPI> workers = new HashMap<String, WorkAPI>();

    public String register(String workerId, String address) {
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(WorkAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://%s:%s/worker", address, workerPort));
        factoryBean.afterPropertiesSet();
        WorkAPI worker = (WorkAPI) factoryBean.getObject();
        String token = UUID.randomUUID().toString();
        workers.put(token, worker);
        System.out.println(String.format("A worker(%s) at %s registered, and assigned with token(%s)",
                                         workerId, address, token));
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
        // pick a worker to perform the job if no cached result
        WorkAPI worker = randomPick();
        if( worker == null )
            throw new IllegalStateException("There is no worker available to perform the job: " + job);
        result = worker.perform(job);
        // store the result to be reused later
        cacheService.store(job, result);
        System.out.println(String.format("Worker perform job(%s) with effort %s", job, result));
        return result;
    }

    private WorkAPI randomPick() {
        int max = workers.size();
        int randIndex = new Random().nextInt(max);
        return (WorkAPI) workers.values().toArray()[randIndex];
    }
}
