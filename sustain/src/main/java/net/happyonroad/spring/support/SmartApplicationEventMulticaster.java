/**
 * Developer: Kadvin Date: 14-6-6 下午2:31
 */
package net.happyonroad.spring.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.event.SmartApplicationListener;

import java.util.HashSet;
import java.util.Set;

/**
 * 能够根据Listener的Event的Source Type进行过滤的派发器
 * <p/>
 * 另外，可以防止事件重复
 */
public class SmartApplicationEventMulticaster extends SimpleApplicationEventMulticaster {
    public SmartApplicationEventMulticaster(BeanFactory beanFactory) {
        super(beanFactory);
    }

    private Set<ApplicationEvent> events = new HashSet<ApplicationEvent>();

    protected boolean supportsEvent(ApplicationListener<?> listener,
                                    Class<? extends ApplicationEvent> eventType, Class<?> sourceType) {

        SmartApplicationListener smartListener = (listener instanceof SmartApplicationListener ?
                                                  (SmartApplicationListener) listener :
                                                  new SmartApplicationListenerAdapter(listener));
        return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
    }

    @Override
    public void multicastEvent(ApplicationEvent event) {
        //防止死循环导致的StackOverFlow, eg:
        // Platform Context --forwarder--> Spring Mvc Context
        // Spring Mvc Context -->multicast--> Parent Context(Platform Context)
        // ...
        if (remembered(event)) return;
        remember(event);
        super.multicastEvent(event);
    }


    private boolean remembered(ApplicationEvent event) {
        return events.contains(event);
    }

    private void remember(ApplicationEvent event) {
        events.add(event);
    }
}
