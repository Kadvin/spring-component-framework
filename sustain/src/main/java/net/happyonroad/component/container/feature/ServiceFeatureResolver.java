/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.container.support.ComponentInputStreamResource;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.Features;
import net.happyonroad.spring.ContextStoppingEvent;
import net.happyonroad.spring.ServiceApplicationContext;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

/** 加载服务组件 */
public class ServiceFeatureResolver extends SpringFeatureResolver {

    // Before application feature resolver load
    // Before application feature resolver unload
    public ServiceFeatureResolver() {
        super(25, 65);
    }

    @Override
    public String getName() {
        return Features.SERVICE_FEATURE;
    }

    @Override
    public boolean hasFeature(Component component) {
        return component.getResource().exists("META-INF/service.xml");
    }

    @Override
    public void resolve(Component component) throws IOException {
        logger.debug("Resolving {} {} feature", component, getName());
        //根据service.xml为其构建parent context
        ComponentResource resource = component.getResource();
        InputStream serviceStream = resource.getServiceStream();
        try {
            ClassRealm realm = resolveContext.getClassRealm(component.getId());
            InputStream applicationStream = resource.getApplicationStream();
            try {
                AbstractApplicationContext parentContext = combineDependedApplicationAsParentContext(component);
                AbstractApplicationContext serviceContext =
                        resolveServiceContext(component, realm, serviceStream, parentContext);
                // 父上下文Start之后，应该为子下文准备好所有要import的对象
                // 所以，父上下文的 service package对象，应该在start事件内将所有import的bean导入过来
                serviceContext.start();
                resolveContext.registerFeature(component, getName(), serviceContext);
                //如果component里面有application.xml，其稍后会自动解析，并予以start
                // 子上下文start之后，也就是其准备好了export服务
                //父上下文会听到子上下文的started事件，转发给Service Package
                // Service package判断其事件类型
                // 如果是started，则 export services
            } finally {
                applicationStream.close();
            }
        } finally {
            serviceStream.close();
        }
        //再根据 application.xml 为其构建 application context(with parent)
        //而将service暴露出去，在这里并不需要有什么实际的动作，暴露应该是在别人来引用时，可以通过隔离审查
    }


    protected AbstractApplicationContext resolveServiceContext(Component component,
                                                               ClassRealm realm,
                                                               InputStream serviceStream,
                                                               AbstractApplicationContext parent) {
        //  这里要根据Service的解析，尤其是 Import的解析情况，决定给予什么样的Resource Loader
        //    这个Resource Loader，很有可能是面向 整个world或者loader的loader
        //    主要要达成的目的是，能够通过某个中介，将服务export过来，并且import过去
        ServiceApplicationContext context = new ServiceApplicationContext(component, realm, parent);
        InputStreamResource resource = new ComponentInputStreamResource(component, serviceStream, "META-INF/service.xml");
        context.load(resource);
        //registerApplicationHelpers(component, context, realm);
        registerServiceHelpers(context);
        context.refresh();
        return context;
    }

    @Override
    public Object release(Component component) {
        AbstractApplicationContext context = (AbstractApplicationContext) super.release(component);
        if( context != null ){
            context.publishEvent(new ContextStoppingEvent(context));
        }
        return context;
    }
}
