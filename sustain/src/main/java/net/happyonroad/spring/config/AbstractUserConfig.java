/**
 * Developer: Kadvin Date: 15/2/3 上午10:11
 */
package net.happyonroad.spring.config;

import net.happyonroad.spring.exception.ServiceConfigurationException;
import net.happyonroad.spring.service.ServiceImporter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <h1>抽象的使用其他服务的应用配置</h1>
 * 主要功能是：
 * <ul>
 *     <li>注入 Service Importer</li>
 *     <li>提供 简便的 imports方法</li>
 * </ul>
 */
public abstract class AbstractUserConfig {
    @Autowired
    protected ServiceImporter importer;

    /**
     * <h2>导入某个服务</h2>
     *
     * @param serviceClass 服务的接口
     * @param <T>          服务的类型
     * @return 服务的实例
     * @throws net.happyonroad.spring.exception.ServiceConfigurationException 找不到服务时抛出
     */
    protected <T> T imports(Class<T> serviceClass) throws ServiceConfigurationException{
        return importer.imports(serviceClass);
    }

    /**
     * <h2>导入某个服务</h2>
     *
     * @param serviceClass 服务的接口
     * @param hint         服务的提示
     * @param <T>          服务的类型
     * @return 服务的实例
     * @throws net.happyonroad.spring.exception.ServiceConfigurationException 找不到服务时抛出
     */
    protected <T> T imports(Class<T> serviceClass, String hint) throws ServiceConfigurationException{
        return importer.imports(serviceClass, hint);
    }

}
