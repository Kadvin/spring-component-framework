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
    //最多存储5m内的事件，5分钟前的事件就删除
    // 避免这里内存泄露
    private static final long MAX_STORAGE_TIME = 1000 * 60 * 5;

    public SmartApplicationEventMulticaster(BeanFactory beanFactory) {
        super(beanFactory);
    }

    private final Set<ApplicationEvent> events = new HashSet<ApplicationEvent>();

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
        //clearExpires();
        super.multicastEvent(event);
    }


    private boolean remembered(ApplicationEvent event) {
        return events.contains(event);
    }

    private void remember(ApplicationEvent event) {
        events.add(event);
    }

    void clearExpires() {
        //TODO 怎么弄，这里都在前端访问触发时抛出 ConcurrentModificationException
        List<ApplicationEvent> removing = new LinkedList<ApplicationEvent>();
        //没有专门配置一个定时清理的任务，而是在某次事件发生时顺带做事件清理
        for (ApplicationEvent evt : new HashSet<ApplicationEvent>(events)) {
            long lives = System.currentTimeMillis() - evt.getTimestamp();
            if (lives > MAX_STORAGE_TIME) {
                removing.add(evt);
            }
        }
        if( !removing.isEmpty() ){
             synchronized (events){
                for (ApplicationEvent evt : removing) {
                    events.remove(evt);
                }
             }
        }
    }
}
