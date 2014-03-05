/**
 * @author XiongJie, Date: 13-10-29
 */

package net.happyonroad.component.container;

import java.util.Map;

/** 可修改的服务注册表 */
public interface MutableServiceRegistry extends ServiceRegistry {

    /**
     * 以默认的hint注册服务的实现
     *
     * @param interfaceClass 服务接口
     * @param service        服务实例
     * @param <T>            服务类型
     */
    <T> void register(Class<T> interfaceClass, T service);

    /**
     * 以特定的hint注册服务的实现
     *
     * @param interfaceClass 服务接口
     * @param service        服务实例
     * @param <T>            服务类型
     * @param hint           服务备注
     */
    <T> void register(Class<T> interfaceClass, T service, String hint);


    /**
     * 取消特定的hint服务的注册
     *
     * @param interfaceClass 服务接口
     * @param <T>            服务类型
     * @param hint           服务备注
     * @return 先前注册的服务对象，如果没有注册过，则返回null
     */
    <T> T unRegister(Class<T> interfaceClass, String hint);

    /**
     * 取消所有特定服务接口的实现
     *
     * @param interfaceClass 服务接口
     * @param <T>            服务类型
     * @return 所有的服务，如果不存在，也会返回一个空的list
     */
    <T> Map<String, T> unRegister(Class<T> interfaceClass);
}
