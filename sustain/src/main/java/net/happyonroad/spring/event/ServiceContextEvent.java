/**
 * Developer: Kadvin Date: 14-4-4 上午11:29
 */
package net.happyonroad.spring.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * The service context event
 */
public class ServiceContextEvent extends ApplicationContextEvent{
    public ServiceContextEvent(ApplicationContext source) {
        super(source);
    }

}
