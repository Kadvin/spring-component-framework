/**
 * Developer: Kadvin Date: 15/2/2 上午11:02
 */
package net.happyonroad.spring.service;

/**
 * <h1>服务导出工具</h1>
 */
public interface ServiceExporter {
    /**
     * <h2>暴露某个服务</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param service      实际的服务实例（不能为空)
     * @param hint         服务的注释
     * @param <T>          服务的类型
     */
    <T> void exports(Class<T> serviceClass, T service, String hint);

    /**
     * <h2>暴露某个服务(hint = default)</h2>
     *
     * @param serviceClass 需要暴露的服务接口
     * @param service      实际的服务实例（不能为空)
     * @param <T>          服务的类型
     */
    <T> void exports(Class<T> serviceClass, T service);
}
