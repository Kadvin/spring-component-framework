/**
 * @author XiongJie, Date: 13-10-29
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.container.MutableServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** 缺省的服务注册表 */
@SuppressWarnings("unchecked")
public class DefaultServiceRegistry implements MutableServiceRegistry {
    protected Logger                          logger   = LoggerFactory.getLogger(getClass());
    private   Map<Class, Map<String, Object>> services = new HashMap<Class, Map<String, Object>>();

    ////////////////////////////////////////////////////////////////////////
    // Mutable Service Registry的实现
    ////////////////////////////////////////////////////////////////////////

    @Override
    public <T> void register(Class<T> interfaceClass, T service) {
        register(interfaceClass, service, DEFAULT_HINT);
    }

    @Override
    public <T> void register(Class<T> interfaceClass, T service, String hint) {
        String serviceName = service.getClass().getSimpleName();
        logger.info("Register {} by {} with hint {}", interfaceClass.getName(), serviceName, hint);
        Map<String, Object> instances = services.get(interfaceClass);
        if (instances == null) {
            instances = new HashMap<String, Object>(2);
            services.put(interfaceClass, instances);
        }
        instances.put(hint, service);
    }

    @Override
    public <T> T unRegister(Class<T> interfaceClass, String hint) {
        List<T> removes = new ArrayList<T>();
        Set<Class> keys = services.keySet();
        for (Class key : keys) {
            if (interfaceClass.isAssignableFrom(key)) {
                Map<String, Object> theMap = services.get(key);
                if (theMap != null) {
                    T removed = (T) theMap.remove(hint);
                    if (removed != null) removes.add(removed);
                }
            }
        }
        if (removes.size() == 1) {
            return removes.get(0);
        } else if (removes.isEmpty()) {
            return null;
        } else {
            logger.warn("I have unRegister multiple service with hint {}", hint);
            return removes.get(0);
        }
    }

    @Override
    public <T> Map<String, T> unRegister(Class<T> interfaceClass) {
        Set<Class> keys = services.keySet();
        Map<String, T> removes = new HashMap<String, T>();
        for (Class key : keys) {
            if (interfaceClass.isAssignableFrom(key)) {
                Map<String, T> theMap = (Map<String, T>) services.remove(key);
                if (theMap != null) {
                    removes.putAll(theMap);
                }
            }
        }
        return removes;
    }

    ////////////////////////////////////////////////////////////////////////
    // Service Registry的实现
    ////////////////////////////////////////////////////////////////////////

    @Override
    public <T> T getService(Class<T> requiredType) {
        return getService(requiredType, ANY_HINT);
    }

    @Override
    public <T> T getService(Class<T> requiredType, String hint) {
        Map<String, Object> map = services.get(requiredType);
        if (map == null) {
            //如果精确查找找不到，则进行模糊匹配，找到第一个算数
            map = digMaps(requiredType);
        }
        if(map.isEmpty()) return null;
        if(ANY_HINT.equals(hint)){
            return (T) map.values().iterator().next();
        }
        return (T) map.get(hint);
    }

    @Override
    public <T> Map<String, T> getServices(Class<T> requiredType) {
        Map<String, Object> map = digMaps(requiredType);
        return (Map<String, T>) map;
    }

    /**
     * 找到符合相应类型的服务map
     *
     * @param requiredType 服务类型
     * @param <T>          服务类型
     * @return 第一个找到的map
     */
    private <T> Map<String, Object> digMaps(Class<T> requiredType) {
        Map<String, Object> all = new HashMap<String, Object>();
        Set<Class> keys = services.keySet();
        for (Class serviceClass : keys) {
            if (requiredType.isAssignableFrom(serviceClass)) {
                //如果出现子类型中的服务与已有存在的冲突了怎么办？
                Map<String, Object> map = services.get(serviceClass);
                for (String key : map.keySet()) {
                    Object service = map.get(key);
                    Object exist = all.get(key);
                    if (exist != null) {
                        logger.warn("A service instance: {} override another: {} with hint = {}", service, exist, key);
                    }
                    all.put(key, service);
                }
            }
        }
        return all;
    }
}
