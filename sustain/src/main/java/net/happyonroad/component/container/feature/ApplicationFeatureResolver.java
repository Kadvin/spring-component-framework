/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.container.support.ComponentInputStreamResource;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.Features;
import net.happyonroad.spring.context.AnnotationComponentApplicationContext;
import net.happyonroad.spring.context.XmlComponentApplicationContext;
import net.happyonroad.spring.exception.ApplicationConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 加载应用组件
 */
public class ApplicationFeatureResolver extends SpringFeatureResolver {
    public static final String APP_CONFIG = "App-Config";
    public static final String APP_XML = "META-INF/application.xml";

    public ApplicationFeatureResolver() {
        super(30, 70);
    }

    @Override
    public String getName() {
        return Features.APPLICATION_FEATURE;
    }

    @Override
    public boolean hasFeature(Component component) {
        return byConfig(component) || byXml(component);
    }

    @Override
    public void resolve(Component component) throws IOException {
        logger.debug("Resolving {} {} feature", component, getName());
        ClassRealm realm = resolveContext.getClassRealm(component.getId());
        ApplicationContext parent = resolveContext.getServiceFeature(component);
        if (parent == null)
            parent = combineDependedApplicationAsParentContext(component);
        AbstractApplicationContext context;
        if( byConfig(component)){
            context = resolveByConfig(component, realm, parent);
        }else{
            context = resolveByXml(component, realm, parent);
        }
        //registerApplicationHelpers(component, context, realm);
        registerServiceHelpers(context);
        context.refresh();
        context.start();
        resolveContext.registerFeature(component, getName(), context);
    }

    /**
     * XML方式解析独立的应用程序包
     *
     * @param component 组件对象
     * @param realm     类环境
     * @return 加载出来的应用程序上下文
     */
    protected AbstractApplicationContext resolveByXml(Component component,
                                                      ClassRealm realm,
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
                                                         ClassRealm realm,
                                                         ApplicationContext parent) {
        String appConfig = component.getResource().getManifest().getMainAttributes().getValue(APP_CONFIG);
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
        ComponentResource resource = component.getResource();
        String appConfig = resource.getManifest().getMainAttributes().getValue(APP_CONFIG);
        return StringUtils.isNotBlank(appConfig);
    }

    protected boolean byXml(Component component) {
        ComponentResource resource = component.getResource();
        return resource.exists(APP_XML);
    }

}
