/**
 * Developer: Kadvin Date: 14-5-29 上午9:56
 */
package net.happyonroad.spring.service;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;

/**
 * 默认的服务配置
 */
public abstract class AbstractServiceConfig {

    private ManagedList<BeanDefinition> exportServices = new ManagedList<BeanDefinition>();
    private ManagedList<BeanDefinition> importServices = new ManagedList<BeanDefinition>();

    public final BeanDefinition servicePackageDefinition() {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SpringServicePackage.class);
        builder.addPropertyValue("exporters", exportServices);
        builder.addPropertyValue("importers", importServices);
        return builder.getBeanDefinition();
    }

    public abstract void defineServices();

    /**
     * 定义需要暴露的服务
     *
     * @param serviceInterface 服务接口名称
     * @param hintAndRef 服务的hint和引用的对象名称，均可以留空
     */
    protected final void exportService(Class serviceInterface, String... hintAndRef) {
        exportService(new Class[]{serviceInterface}, hintAndRef);
    }

    /**
     * 定义需要暴露的服务
     *
     * @param serviceInterfaces 服务接口名称集合
     * @param hintAndRef 服务的hint和引用的对象名称，均可以留空
     */
    protected final void exportService(Class[] serviceInterfaces, String... hintAndRef) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SpringServiceExporter.class);
        builder.addPropertyValue("roleClass", serviceInterfaces);
        if (hintAndRef.length > 0) builder.addPropertyValue("hint", hintAndRef[0]);
        if (hintAndRef.length > 1) builder.addPropertyValue("ref", hintAndRef[1]);
        exportServices.add(builder.getBeanDefinition());
    }

    /**
     * 定义需要引入的服务
     *
     * @param serviceInterface 服务接口名称
     * @param hintAndAs 服务的hint和引入后的对象名称，均可以留空
     */
    protected final void importService(Class serviceInterface, String... hintAndAs){
        importService(new Class[]{serviceInterface}, hintAndAs);
    }

    /**
     * 定义需要引入的服务
     *
     * @param serviceInterfaces 服务接口名称集合
     * @param hintAndAs 服务的hint和引入后的对象名称，均可以留空
     */
    protected final void importService(Class[] serviceInterfaces, String... hintAndAs){
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SpringServiceImporter.class);
        builder.addPropertyValue("roleClass", serviceInterfaces);
        if( hintAndAs.length > 0 ) builder.addPropertyValue("hint", hintAndAs[0]);
        if( hintAndAs.length > 1 ) builder.addPropertyValue("as", hintAndAs[1]);
        importServices.add(builder.getBeanDefinition());
    }

}
