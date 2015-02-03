/**
 * Developer: Kadvin Date: 15/2/3 上午10:02
 */
package net.happyonroad.spring.service;

/**
 * <h1>基于某个本地Application Context的Service exporter</h1>
 */
public interface LocalServiceExporter {
    /**
     * <h2>暴露某个服务</h2>
     *
     * 服务实例将会从本地context寻找
     *
     * @param serviceClass 需要暴露的服务接口
     * @param hint         服务的注释
     * @param <T>          服务的类型
     */
    <T> void exports(Class<T> serviceClass, String hint);

    /**
     * <h2>暴露某个服务(hint = default)</h2>
     * 服务实例将会从本地context寻找
     *
     * @param serviceClass 需要暴露的服务接口
     * @param <T>          服务的类型
     */
    <T> void exports(Class<T> serviceClass);
}
