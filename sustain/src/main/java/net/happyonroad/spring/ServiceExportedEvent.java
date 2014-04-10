/**
 * Developer: Kadvin Date: 14-4-4 上午11:08
 */
package net.happyonroad.spring;

import org.springframework.context.ApplicationContext;

/**
 * The services are exported
 */
public class ServiceExportedEvent extends ServiceContextEvent {
    public ServiceExportedEvent(ApplicationContext source) {
        super(source);
    }
}
