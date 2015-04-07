/**
 * Developer: Kadvin Date: 14-6-6 下午2:31
 */
package net.happyonroad.spring.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.event.SmartApplicationListener;

import java.util.*;

/**
 * 能够根据Listener的Event的Source Type进行过滤的派发器
 * <p/>
 * 另外，可以防止事件重复
 */
public class SmartApplicationEventMulticaster extends SimpleApplicationEventMulticaster {
    public SmartApplicationEventMulticaster(BeanFactory beanFactory) {
        super(beanFactory);
    }
    //最多存储5m内的事件，5分钟前的事件就删除
    // 避免这里内存泄露
    private static final long                  MAX_STORAGE_TIME = 1000 * 60 * 5;
    private final        Set<ApplicationEvent> events           = new HashSet<ApplicationEvent>();

    public Collection<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event){
        return super.getApplicationListeners(event);
    }

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
        clearExpires();
        super.multicastEvent(event);
    }


    private boolean remembered(ApplicationEvent event) {
        return events.contains(event);
    }

    private void remember(ApplicationEvent event) {
        events.add(event);
    }

    void clearExpires() {
        if( events.size() < 100 ) return;
        try {
            Iterator<ApplicationEvent> it = events.iterator();
            while (it.hasNext()) {
                ApplicationEvent event = it.next();
                long lives = System.currentTimeMillis() - event.getTimestamp();
                if (lives > MAX_STORAGE_TIME) {
                    it.remove();
                }
            }
        } catch (Exception e) {
            //skip concurrent modification error
        }
    }
}
