/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.container.RepositoryScanner;
import net.happyonroad.component.container.support.ComponentInputStreamResource;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.Features;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.spring.context.AnnotationComponentApplicationContext;
import net.happyonroad.spring.context.XmlComponentApplicationContext;
import net.happyonroad.spring.event.ComponentLoadedEvent;
import net.happyonroad.spring.exception.ApplicationConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.context.ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;

/**
 * 加载应用组件
 */
public class ApplicationFeatureResolver extends AbstractFeatureResolver {
    public static final String APP_CONFIG = "App-Config";
    public static final String APP_REPOSITORY = "App-Repository";
    public static final String APP_XML = "META-INF/application.xml";

    public ApplicationFeatureResolver() {
        super(30, 70);
    }

    @Override
    public String getName() {
        return Features.APPLICATION_FEATURE;
    }

    @Override
    public void applyDefaults(Component component) {
        super.applyDefaults(component);
        String appConfig = component.getManifestAttribute(APP_CONFIG);
        if( appConfig == null && readComponentDefaultConfig(component, "A").contains("A")){
            appConfig = System.getProperty("default.app.config", "net.happyonroad.spring.config.DefaultAppConfig");
        }
        component.setManifestAttribute(APP_CONFIG, appConfig);
    }

    @Override
    public boolean hasFeature(Component component) {
        return byConfig(component) || byXml(component);
    }

    @Override
    public void resolve(Component component) throws IOException {
        logger.debug("Resolving {} {} feature", component, getName());
        ClassLoader realm = resolveContext.getClassRealm(component.getId());
        ApplicationContext parent = resolveContext.getRootContext() ;
        AbstractApplicationContext context;
        if( byConfig(component)){
            context = resolveByConfig(component, realm, parent);
        }else{
            context = resolveByXml(component, realm, parent);
        }
        String env = System.getProperty("spring.env", "production");
        context.getEnvironment().setActiveProfiles(env);
        if( component.getScanners() != null ){
            for (RepositoryScanner scanner : component.getScanners()) {
                scanner.bind(context);
                scanner.scan();
            }
        }
        scanAppRepository(context, component);
        //registerApplicationHelpers(component, context, realm);
        context.refresh();
        ((DefaultComponent)component).setApplication(context);
        context.start();
        resolveContext.registerFeature(component, getName(), context);
    }

    private void scanAppRepository(AbstractApplicationContext context, Component component) {
        String appRepository = component.getManifestAttribute(APP_REPOSITORY);
        if( StringUtils.isEmpty(appRepository)){
            return;
        }
        int count;
        if( context instanceof AnnotationConfigRegistry){
            int before = context.getBeanDefinitionCount();
            ((AnnotationConfigRegistry) context).scan(StringUtils.split(appRepository, CONFIG_LOCATION_DELIMITERS));
            count = context.getBeanDefinitionCount() - before;
        }else{
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) context);
            scanner.setEnvironment(context.getEnvironment());
            scanner.setResourceLoader(context);
            scanner.setIncludeAnnotationConfig(false);
            count = scanner.scan(StringUtils.split(appRepository, CONFIG_LOCATION_DELIMITERS));
        }
        if(count > 0 && logger.isDebugEnabled()){
            logger.debug("Scanned {} beans in {}", count, component.getDisplayName());
            String[] names = context.getBeanDefinitionNames();
            for (String name : names) {
                BeanDefinition definition = ((GenericApplicationContext)context).getBeanDefinition(name);
                if( definition instanceof ScannedGenericBeanDefinition){
                    ScannedGenericBeanDefinition sgbd = (ScannedGenericBeanDefinition) definition;
                    Class<?> beanClass;
                    try {
                        beanClass = sgbd.resolveBeanClass(context.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        continue;
                    }
                    if( beanClass != null ){
                        logger.debug("\t{}", beanClass.getName());
                    }
                }
            }
        }

    }

    /**
     * XML方式解析独立的应用程序包
     *
     * @param component 组件对象
     * @param realm     类环境
     * @return 加载出来的应用程序上下文
     */
    protected AbstractApplicationContext resolveByXml(Component component,
                                                      ClassLoader realm,
                                                      ApplicationContext parent) {
        InputStream applicationStream = null;
        try {
            applicationStream = component.getResource().getApplicationStream();
            XmlComponentApplicationContext context = new XmlComponentApplicationContext(component, realm, parent);
            InputStreamResource resource = new ComponentInputStreamResource(component, applicationStream, APP_XML);
            try {
                context.load(resource);
            } catch (Exception e) {
                throw new ApplicationConfigurationException("Can't load context from application stream", e);
            }
            return context;
        } catch (IOException e) {
            throw new ApplicationConfigurationException("Can't read " + APP_XML, e);
        } finally {
            IOUtils.closeQuietly(applicationStream);
        }
    }

    /**
     * Annotation方式解析独立的应用程序包
     *
     * @param component 组件对象
     * @param realm     类环境
     * @return 加载出来的应用程序上下文
     */
    protected AbstractApplicationContext resolveByConfig(Component component,
                                                         ClassLoader realm,
                                                         ApplicationContext parent) {
        String appConfig = component.getManifestAttribute(APP_CONFIG);
        Class appConfigClass;
        try {
            appConfigClass = Class.forName(appConfig, true, component.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ApplicationConfigurationException("The app config class: " + appConfig
                                                + " can't be loaded by the component:" + component, e);
        }
        AnnotationComponentApplicationContext context = new AnnotationComponentApplicationContext(component, realm, parent);
        context.register(appConfigClass);
        return context;
    }

    protected boolean byConfig(Component component) {
        String appConfig = component.getManifestAttribute(APP_CONFIG);
        return StringUtils.isNotBlank(appConfig);
    }

    protected boolean byXml(Component component) {
        ComponentResource resource = component.getResource();
        return resource.exists(APP_XML);
    }



    @Override
    public Object release(Component component) {
        AbstractApplicationContext context = (AbstractApplicationContext) super.release(component);
        if (context != null) {
            shutdownContext(context);
        }else{
            logger.error("Can't pick loaded {} feature for: {}", getName(), component);
        }
        return context;
    }

    @Override
    public void afterResolve(Component component) {
        super.afterResolve(component);
        //向其内部所有组件发布通知，其已经被加载
        component.getApplication().publishEvent(new ComponentLoadedEvent(component));
    }

    protected void shutdownContext(AbstractApplicationContext context) {
        context.stop();
        context.close();
    }

}
