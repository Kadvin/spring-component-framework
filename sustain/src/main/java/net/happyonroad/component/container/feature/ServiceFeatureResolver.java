/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.container.support.ComponentInputStreamResource;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.Features;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.spring.context.GenericServiceApplicationContext;
import net.happyonroad.spring.context.XmlServiceApplicationContext;
import net.happyonroad.spring.event.ContextStoppingEvent;
import net.happyonroad.spring.exception.ServiceConfigurationException;
import net.happyonroad.spring.service.AbstractServiceConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 加载服务组件
 */
public class ServiceFeatureResolver extends SpringFeatureResolver {
    public static final String SERVICE_CONFIG = "Service-Config";
    public static final String SERVICE_XML    = "META-INF/service.xml";

    // Before application feature resolver load
    // Before application feature resolver unload
    public ServiceFeatureResolver() {
        // 越小越先执行: ApplicationFeatureResolver(30, 70)
        super(25, 65);
    }

    @Override
    public String getName() {
        return Features.SERVICE_FEATURE;
    }

    @Override
    public boolean hasFeature(Component component) {
        return byConfig(component) || byXml(component);
    }

    @Override
    public void resolve(Component component) throws IOException {
        logger.debug("Resolving {} {} feature", component, getName());
        ClassRealm realm = resolveContext.getClassRealm(component.getId());
        AbstractApplicationContext parentContext = combineDependedApplicationAsParentContext(component);
        //根据service.xml为其构建parent context
        AbstractApplicationContext serviceContext;
        if (byConfig(component)) {
            serviceContext = resolveByConfig(component, realm, parentContext);
        } else {
            serviceContext = resolveByXml(component, realm, parentContext);
        }
        registerServiceHelpers(serviceContext);
        String env = System.getProperty("spring.env", "production");
        serviceContext.getEnvironment().setActiveProfiles(env);
        serviceContext.refresh();
        ((DefaultComponent)component).setServiceApplication(serviceContext);
        // 父上下文Start之后，应该为子下文准备好所有要import的对象
        // 所以，父上下文的 service package对象，应该在start事件内将所有import的bean导入过来
        serviceContext.start();
        resolveContext.registerFeature(component, getName(), serviceContext);
        //如果component里面有application.xml，其稍后会自动解析，并予以start
        // 子上下文start之后，也就是其准备好了export服务
        //父上下文会听到子上下文的started事件，转发给Service Package
        // Service package判断其事件类型
        // 如果是started，则 export services
        //再根据 application.xml 为其构建 application context(with parent)
        //而将service暴露出去，在这里并不需要有什么实际的动作，暴露应该是在别人来引用时，可以通过隔离审查
    }


    @Override
    public Object release(Component component) {
        AbstractApplicationContext context = (AbstractApplicationContext) super.release(component);
        if (context != null) {
            context.publishEvent(new ContextStoppingEvent(context));
        }
        return context;
    }

    protected AbstractApplicationContext resolveByXml(Component component,
                                                      ClassRealm realm,
                                                      AbstractApplicationContext parent) {
        InputStream serviceStream = null;
        try {
            serviceStream = component.getResource().getServiceStream();
            //  这里要根据Service的解析，尤其是 Import的解析情况，决定给予什么样的Resource Loader
            //    这个Resource Loader，很有可能是面向 整个world或者loader的loader
            //    主要要达成的目的是，能够通过某个中介，将服务export过来，并且import过去
            XmlServiceApplicationContext context = new XmlServiceApplicationContext(component, realm, parent);
            InputStreamResource resource = new ComponentInputStreamResource(component, serviceStream, SERVICE_XML);
            context.load(resource);
            return context;
        } catch (IOException e) {
            throw new ServiceConfigurationException("Can't read: " + SERVICE_XML, e);
        } finally {
            IOUtils.closeQuietly(serviceStream);
        }
    }

    protected AbstractApplicationContext resolveByConfig(Component component,
                                                         ClassRealm realm,
                                                         AbstractApplicationContext parent) {
        String serviceConfigName = component.getManifestAttribute(SERVICE_CONFIG);
        Class<? extends AbstractServiceConfig> serviceConfigClass;
        try {
            //noinspection unchecked
            serviceConfigClass = (Class<? extends AbstractServiceConfig>) Class.forName(serviceConfigName, true,
                                                                                        component.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationException("The service config class: "
                                                    +serviceConfigName+ " can't be loaded by: " + component);
        }
        AbstractServiceConfig serviceConfig;
        try {
            serviceConfig = serviceConfigClass.newInstance();
        } catch (Exception e) {
            throw new ServiceConfigurationException("The service config should have a public empty constructor", e);
        }
        try {
            serviceConfig.defineServices();
        } catch (Exception e) {
            throw new ServiceConfigurationException("Error while define services for:" + serviceConfigName, e);
        }
        //  这里要根据Service的解析，尤其是 Import的解析情况，决定给予什么样的Resource Loader
        //    这个Resource Loader，很有可能是面向 整个world或者loader的loader
        //    主要要达成的目的是，能够通过某个中介，将服务export过来，并且import过去
        GenericServiceApplicationContext context = new GenericServiceApplicationContext(component, realm, parent);
        BeanDefinition beanDefinition = serviceConfig.servicePackageDefinition();
        context.registerBeanDefinition("springServicePackage", beanDefinition);
        return context;
    }

    protected boolean byConfig(Component component) {
        String serviceConfig = component.getManifestAttribute(SERVICE_CONFIG);
        return StringUtils.isNotBlank(serviceConfig) ;
    }

    protected boolean byXml(Component component) {
        ComponentResource resource = component.getResource();
        return resource.exists(SERVICE_XML);
    }

}
