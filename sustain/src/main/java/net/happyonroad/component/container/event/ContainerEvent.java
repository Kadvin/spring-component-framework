/**
 * Developer: Kadvin Date: 14-4-8 上午10:17
 */
package net.happyonroad.component.container.event;

import org.springframework.context.ApplicationEvent;

/**
 * All kinds of container event
 */
public class ContainerEvent extends ApplicationEvent {
    public ContainerEvent(Object source) {
        super(source);
    }
}
