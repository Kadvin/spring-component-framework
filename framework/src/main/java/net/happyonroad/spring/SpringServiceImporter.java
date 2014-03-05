/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

import net.happyonroad.component.container.MutableServiceRegistry;
import net.happyonroad.component.container.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/** 导入服务的Bean对象 */
public class SpringServiceImporter extends SpringServiceProxy {
    private static Logger logger = LoggerFactory.getLogger(SpringServiceImporter.class);

    private String as;

    public String getAs() {
        if(!StringUtils.hasText(as)){
            String last = StringUtils.unqualify(getRole());
            return   StringUtils.uncapitalize(last);
        }
        return as;
    }

    public void setAs(String as) {
        this.as = as;
    }

    @Override
    public String getHint() {
        String hint = super.getHint();
        if (!StringUtils.hasText(hint)) {
            return ServiceRegistry.ANY_HINT;
        }
        return hint;
    }

    /**
     * 从服务注册表中导入相应的服务，并放在特定上下文中
     *
     * @param serviceRegistry  服务注册表
     * @param serviceContext 服务上下文，不是组件的那个上下文
     */
    public void importService(MutableServiceRegistry serviceRegistry, ApplicationContext serviceContext)
            throws ServiceNotFoundException {
        Object service = serviceRegistry.getService(getRoleClass(), getHint());
        if(service != null){
            ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) serviceContext.getAutowireCapableBeanFactory();
            logger.debug("Import {} to {} as {}", this, serviceContext.getDisplayName(), getAs());
            cbf.registerSingleton(getAs(), service);
        }else{
            throw new ServiceNotFoundException(this);
        }
    }

    /**
     * 从组件上下文中移除特定的服务
     *
     * @param serviceRegistry  服务注册表
     * @param serviceContext 服务上下文，不是组件的那个上下文
     */
    public void removeService(MutableServiceRegistry serviceRegistry, ApplicationContext serviceContext) {
        logger.debug("Remove {} which is named as {} from {}", this, getAs(), serviceContext.getDisplayName());
        ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) serviceContext.getAutowireCapableBeanFactory();
        Object service = cbf.getBean(getAs());
        //TODO 现在这样注册过去的bean无法被卸载
        try {
            cbf.destroyBean(getAs(), service);
        } catch (Exception e) {
            logger.trace("TODO: registerResolver a destroyable bean:" + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("%s(hint=%s, as=%s)", ClassUtils.getShortName(role), getHint(), getAs());
    }
}
