/**
 * Developer: Kadvin Date: 15/2/2 下午5:41
 */
package net.happyonroad.spring.event;

import net.happyonroad.component.core.Component;
import org.springframework.context.ApplicationEvent;

/**
 * Component Loaded Event
 */
public class ComponentLoadedEvent extends ApplicationEvent {
    public ComponentLoadedEvent(Component source) {
        super(source);
    }

    @Override
    public Component getSource() {
        return (Component) super.getSource();
    }
}


