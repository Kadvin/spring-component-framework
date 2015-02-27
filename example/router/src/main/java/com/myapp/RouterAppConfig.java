/**
 * Developer: Kadvin Date: 15/2/27 下午1:53
 */
package com.myapp;

import com.myapp.api.RouteAPI;
import net.happyonroad.spring.config.AbstractAppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * Router App Config
 */
@Configuration
@ComponentScan("com.myapp.route")
@Import(BasisUserConfig.class)
@ImportResource("classpath:META-INF/properties.xml")
public class RouterAppConfig extends AbstractAppConfig {
    @Autowired
    RouteAPI routeAPI;
    @Value("${app.port}")
    int routerPort;

    @Bean
    public RmiServiceExporter workAPIExporter() {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(RouteAPI.class);
        exporter.setServiceName("router");
        exporter.setRegistryPort(routerPort);
        exporter.setService(routeAPI);
        return exporter;
    }

}
