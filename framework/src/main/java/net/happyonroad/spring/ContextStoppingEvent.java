/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

/** 上下文即将停止事件 */
public class ContextStoppingEvent extends ApplicationContextEvent {
    public ContextStoppingEvent(ApplicationContext source) {
        super(source);
    }
}
