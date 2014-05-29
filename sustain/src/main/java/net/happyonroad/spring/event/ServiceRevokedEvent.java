/**
 * Developer: Kadvin Date: 14-4-4 上午11:10
 */
package net.happyonroad.spring.event;

import org.springframework.context.ApplicationContext;

/**
 * The services are revoked event
 */
public class ServiceRevokedEvent extends ServiceContextEvent {
    public ServiceRevokedEvent(ApplicationContext source) {
        super(source);
    }
}
