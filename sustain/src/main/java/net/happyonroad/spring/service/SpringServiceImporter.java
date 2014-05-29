/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring.service;

import net.happyonroad.component.container.MutableServiceRegistry;
import net.happyonroad.component.container.ServiceRegistry;
import net.happyonroad.spring.exception.ServiceConfigurationException;
import net.happyonroad.spring.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
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
        Object service = serviceRegistry.getService(getRoleClasses(), getHint());
        if(service != null){
            ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) serviceContext.getAutowireCapableBeanFactory();
            logger.debug("Import {} to {} as {}", this, serviceContext.getDisplayName(), getAs());
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setTarget(service);
            proxyFactory.setInterfaces(getRoleClasses());
            proxyFactory.setOpaque(true);
            Object proxy;
            ClassLoader classLoader = service.getClass().getClassLoader();
            try {
                proxy = proxyFactory.getProxy(classLoader);
            } catch (IllegalArgumentException e) {
                throw new ServiceConfigurationException("Can't create service proxy," +
                                                        " current class loader is " + classLoader, e);
            }
            cbf.registerSingleton(getAs(), proxy);
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
        //TODO HOW TO DEAL WITH IT?
        logger.debug("Remove {} which is named as {} from {}", this, getAs(), serviceContext.getDisplayName());
    }

    @Override
    public String toString() {
        return String.format("%s(hint=%s, as=%s)", ClassUtils.getShortName(role), getHint(), getAs());
    }
}
