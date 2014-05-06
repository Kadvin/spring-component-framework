/**
 * @author XiongJie, Date: 13-10-29
 */

package net.happyonroad.component.container;

import java.util.Map;

/** 服务注册表 */
public interface ServiceRegistry {
    final String DEFAULT_HINT = "default";
    final String ANY_HINT = "*";
    /**
     * 获得服务接口的缺省服务对象(hint = *)
     *
     * @param requiredType 服务的接口
     * @param <T>          服务的类型
     * @return 服务实例
     */
    <T> T getService(Class<T> requiredType);

    /**
     * 获得某个服务接口的服务对象
     *
     * @param requiredType 服务的接口
     * @param <T>          服务的类型
     * @param hint         服务的题注
     * @return 服务实例，如果存在多个，则返回第一个
     */
    <T> T getService(Class<T> requiredType, String hint);

    /**
     * 找到某个同时暴露多个接口的服务bean
     * @param requiredTypes 服务接口
     * @param hint 服务备注
     * @param <T> 服务类型
     * @return 找到的服务对象
     */
    <T> T getService(Class[] requiredTypes, String hint);

    /**
     * 获得某个服务接口所有的实例对象
     *
     * @param requiredType 服务的接口
     * @param <T>          服务的类型
     * @return 所有服务实例
     */
    <T> Map<String, T> getServices(Class<T> requiredType);
}
