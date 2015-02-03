/**
 * Developer: Kadvin Date: 15/2/2 上午11:09
 */
package net.happyonroad.spring.service;

import net.happyonroad.spring.exception.ServiceConfigurationException;

/**
 * <h1>服务导入工具</h1>
 */
public interface ServiceImporter {
    /**
     * <h2>导入某个服务</h2>
     *
     * @param serviceClass 服务的接口
     * @param <T>          服务的类型
     * @return 服务的实例
     * @throws net.happyonroad.spring.exception.ServiceConfigurationException 找不到服务时抛出
     */
    <T> T imports(Class<T> serviceClass) throws ServiceConfigurationException;

    /**
     * <h2>导入某个服务</h2>
     *
     * @param serviceClass 服务的接口
     * @param hint         服务的提示
     * @param <T>          服务的类型
     * @return 服务的实例
     * @throws net.happyonroad.spring.exception.ServiceConfigurationException 找不到服务时抛出
     */
    <T> T imports(Class<T> serviceClass, String hint) throws ServiceConfigurationException;
}
