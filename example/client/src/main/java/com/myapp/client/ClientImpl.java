/**
 * @author XiongJie, Date: 13-11-27
 */
package com.myapp.client;

import com.myapp.api.ClientAPI;
import com.myapp.api.ServerAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.Lifecycle;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * A sample client
 *   which will return some random UUID as work effort
 */
@org.springframework.stereotype.Component
public class ClientImpl implements ClientAPI, Lifecycle {
    @Value("${server.app.port}")
    private       int     serverPort;
    private final String  id;
    private final String  ip;
    private       boolean running;

    public ClientImpl() {
        this.id = UUID.randomUUID().toString();
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Can't get local ip address");
        }
    }

    @Override
    public Object perform(String job) {
        UUID uuid = UUID.randomUUID();
        System.out.println(String.format("Return server job %s with %s", job, uuid.toString()));
        return uuid;
    }

    @Override
    public void start() {
        this.running = true;
        RmiProxyFactoryBean factoryBean = new RmiProxyFactoryBean();
        factoryBean.setServiceInterface(ServerAPI.class);
        factoryBean.setServiceUrl(String.format("rmi://localhost:%d/server", serverPort));
        factoryBean.afterPropertiesSet();
        ServerAPI serverAPI = (ServerAPI) factoryBean.getObject();
        serverAPI.register(id, this.ip);
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
