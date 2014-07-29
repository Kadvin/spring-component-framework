/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring.service;

import net.happyonroad.component.container.MutableServiceRegistry;
import net.happyonroad.component.container.ServiceRegistry;
import net.happyonroad.spring.exception.ServiceConfigurationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * <h1>将子component context中的服务暴露出来的Bean对象</h1>
 *
 * 本对象作为一个隐性的Bean定义在父Service Context中，但主要工作是监听子component context的事件
 *
 * <ol>
 * <li>子Component Context Start之后，将需要暴露的对象注册出去
 * <li>子component Context Stop之后，将已经暴露的对象回收回来
 * </ol>
 */
public class SpringServiceExporter extends SpringServiceProxy {

    private String ref;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    @Override
    public String getHint() {
        String hint = super.getHint();
        if (!StringUtils.hasText(hint)) {
            return ServiceRegistry.DEFAULT_HINT;
        }
        return hint;
    }

    /**
     * 将组件上下文中的特定Bean暴露到服务注册表中去
     *
     * @param serviceRegistry  服务注册表
     * @param componentContext 组件上下文，不是服务上下文
     */
    public void exportService(MutableServiceRegistry serviceRegistry, ApplicationContext componentContext)
            throws ServiceConfigurationException {
        Object service;
        try {
            service = getServiceReference(componentContext);
        } catch (NoSuchBeanDefinitionException e) {
            throw new ServiceConfigurationException(
                    "The service ref = " + getRef() + ", type = " + getRole() + " can't been found in " +
                    componentContext.getDisplayName() + "!", e);
        }
        logger.debug("Export {} -> {} from {} to service registry", this, service, componentContext.getDisplayName());
        serviceRegistry.register(getRoleClasses(), service, getHint());
    }

    /**
     * 从服务注册表中回收由组件上下文暴露的特定Bean
     *
     * @param serviceRegistry  服务注册表
     * @param componentContext 组件上下文，不是服务上下文
     */
    public void revokeService(MutableServiceRegistry serviceRegistry, ApplicationContext componentContext) {
        Object service = getServiceReference(componentContext);
        logger.debug("Revoke {} -> {} to {}", this, service, componentContext);
        // TODO 服务从注册表revoke只能避免其被其他使用者再次发现
        // 但对于已经import的对象，将无能为力
        // 实际应该的做法是，在export的时候，就export一个proxy对象
        // 这里对proxy切入一个控制，就是对所有的服务方法，都抛出ServiceRevokedException
        serviceRegistry.unRegister(getRoleClass(), getHint());
    }

    private Object getServiceReference(ApplicationContext componentContext) {
        Object service;
        if(StringUtils.hasText(getRef())){
            service = componentContext.getBean(getRef(), getRoleClass());
        }else{
            service = componentContext.getBean(getRoleClass());
        }
        return service;
    }

    @Override
    public String toString() {
        return String.format("%s(ref=%s, hint=%s)", ClassUtils.getShortName(role), getRef(), getHint());
    }
}
