/**
 * Developer: Kadvin Date: 15/2/27 上午11:09
 */
package com.myapp;

import com.myapp.api.WorkAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * Worker Application Configuration
 */
@Configuration
@ComponentScan("com.myapp.work")
@ImportResource("classpath:META-INF/properties.xml")
public class WorkerAppConfig {

    @Autowired
    WorkAPI workAPI;
    @Value("${app.port}")
    int workerPort;

    @Bean
    public RmiServiceExporter workAPIExporter() {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceInterface(WorkAPI.class);
        exporter.setServiceName("worker");
        exporter.setRegistryPort(workerPort);
        exporter.setService(workAPI);
        return exporter;
    }
}
