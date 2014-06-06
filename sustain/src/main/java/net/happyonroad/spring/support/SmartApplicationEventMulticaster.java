/**
 * Developer: Kadvin Date: 14-6-6 下午2:31
 */
package net.happyonroad.spring.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.event.SmartApplicationListener;

/**
 * 能够根据Listener的Event的Source Type进行过滤的派发器
 */
public class SmartApplicationEventMulticaster extends SimpleApplicationEventMulticaster {
    public SmartApplicationEventMulticaster(BeanFactory beanFactory) {
        super(beanFactory);
    }

    protected boolean supportsEvent(ApplicationListener<?> listener,
   			Class<? extends ApplicationEvent> eventType, Class<?> sourceType) {

   		SmartApplicationListener smartListener = (listener instanceof SmartApplicationListener ?
   				(SmartApplicationListener) listener : new SmartApplicationListenerAdapter(listener));
   		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
   	}

}
