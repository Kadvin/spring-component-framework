/**
 * Developer: Kadvin Date: 14-6-6 下午2:22
 */
package net.happyonroad.spring.support;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 能够根据事件的source类型进行过滤的 application listener 适配器
 */
@SuppressWarnings("ALL")
public class SmartApplicationListenerAdapter implements SmartApplicationListener {
    private final ApplicationListener<ApplicationEvent> delegate;

    public SmartApplicationListenerAdapter(ApplicationListener<?> delegate) {
        Assert.notNull(delegate, "Delegate listener must not be null");
        this.delegate = (ApplicationListener<ApplicationEvent>) delegate;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        this.delegate.onApplicationEvent(event);
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        Class<?> declaredEventType = resolveEventType();
		return (declaredEventType == null || declaredEventType.isAssignableFrom(eventType));
	}

    protected Class<?> resolveEventType() {
        Class<?> declaredEventType = resolveDeclaredEventType(this.delegate.getClass());
        if (declaredEventType == null || declaredEventType.equals(ApplicationEvent.class)) {
            Class<?> targetClass = AopUtils.getTargetClass(this.delegate);
			if (targetClass != this.delegate.getClass()) {
				declaredEventType = resolveDeclaredEventType(targetClass);
			}
		}
        return declaredEventType;
    }

    @Override
	public boolean supportsSourceType(Class<?> sourceType) {
        // ApplicationListener<ModelEvent<Resource>>
        Type declaredListenerType = declaredApplicationListener(this.delegate.getClass());
        if( declaredListenerType instanceof ParameterizedType){
            Type[] eventTypes = ((ParameterizedType) declaredListenerType).getActualTypeArguments();
            if( eventTypes.length == 0 ) return true;
            //ModelEvent<Model>
            Type eventType = eventTypes[0];
            if( eventType instanceof ParameterizedType){
                Type[] modelTypes = ((ParameterizedType) eventType).getActualTypeArguments();
                if( modelTypes.length == 0 ) return true;
                Type modelType = modelTypes[0];
                Class modelClass;
                if( modelType instanceof ParameterizedType){
                    modelClass = (Class)((ParameterizedType) modelType).getRawType();
                }else if (modelType instanceof WildcardType){
                    WildcardType wildType = (WildcardType) modelType;
                    Type bound = wildType.getLowerBounds().length > 0 ?
                                 wildType.getLowerBounds()[0] :
                                 wildType.getUpperBounds()[0];
                    modelClass = (Class) bound;
                }else {
                    modelClass = (Class) modelType;
                }
                return modelClass.isAssignableFrom(sourceType);
            }
        }
        return true;
	}

    private Type declaredApplicationListener(Class<?> theClass) {
        // 错误情况，怎么会找不到对 application listener的接口声明?
        if( theClass == Object.class )
            throw new IllegalStateException("Can't find the delegate impl ApplicationListener");
        Type[] itfs = theClass.getGenericInterfaces();
        for (Type itf : itfs) {
            Class itfClass;
            if( itf instanceof ParameterizedType){
                itfClass = (Class) ((ParameterizedType) itf).getRawType();
            }else {
                itfClass = (Class) itf;
            }
            if( ApplicationListener.class.isAssignableFrom(itfClass) ){
                return itf;
            }
        }
        return declaredApplicationListener(theClass.getSuperclass());
    }

    @Override
	public int getOrder() {
		return (this.delegate instanceof Ordered ? ((Ordered) this.delegate).getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	static Class<?> resolveDeclaredEventType(Class<?> listenerType) {
		return GenericTypeResolver.resolveTypeArgument(listenerType, ApplicationListener.class);
	}

	static Class<?> resolveDeclaredSourceType(Class<?> eventType) {
		return GenericTypeResolver.resolveTypeArgument(eventType, Object.class);
	}

}
