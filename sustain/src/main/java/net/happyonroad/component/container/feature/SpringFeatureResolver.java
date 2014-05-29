/**
 * @author XiongJie, Date: 13-11-12
 */
package net.happyonroad.component.container.feature;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.Features;
import net.happyonroad.spring.context.CombinedApplicationContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.LinkedHashSet;
import java.util.Set;

/** Resolve the general spring application context */
public abstract class SpringFeatureResolver extends AbstractFeatureResolver {
    public SpringFeatureResolver(int loadOrder, int unloadOrder) {
        super(loadOrder, unloadOrder);
    }

    protected AbstractApplicationContext combineDependedApplicationAsParentContext(Component component) {
        // 将所有该组件依赖的组件生成的context组合起来，作为parent context，以便直接获取相关设置
        Set<ApplicationContext> dependedContexts = new LinkedHashSet<ApplicationContext>();
        digDependedApplicationContext(component, dependedContexts);
        if (dependedContexts.isEmpty())
            return null;
        else {
            CombinedApplicationContext combined = new CombinedApplicationContext(dependedContexts);
            combined.refresh();
            combined.start();
            return combined;
        }
    }


    protected void digDependedApplicationContext(Component component, Set<ApplicationContext> dependedContexts) {
        Object loaded = resolveContext.getFeature(component, Features.APPLICATION_FEATURE);
        if (loaded != null && loaded instanceof ApplicationContext) {
            ApplicationContext componentContext = (ApplicationContext) loaded;
            dependedContexts.add(componentContext);
        }
        for (Component depended : component.getDependedComponents()) {
            digDependedApplicationContext(depended, dependedContexts);
        }
    }

//    protected void registerApplicationHelpers(Component component,
//                                          GenericXmlApplicationContext context,
//                                          ClassRealm realm) {
        //ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) context.getAutowireCapableBeanFactory();
        //将组件注册到服务上下文中

        //cbf.registerSingleton("component", component);
        //将组件的资源加载器也注册到其中
        //cbf.registerSingleton("realm", realm);
        //将world也注册进去
        //cbf.registerSingleton("world", resolveContext.getWorld());
//    }

    protected void registerServiceHelpers(AbstractApplicationContext context) {
        ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) context.getAutowireCapableBeanFactory();
        //将全局的注册表也注册进去
        cbf.registerSingleton("serviceRegistry", resolveContext.getRegistry());
        //将Component Loader也注册进去
        cbf.registerSingleton("componentLoader", resolveContext.getComponentLoader());
        //将Component Repository也注册进去
        cbf.registerSingleton("componentRepository", resolveContext.getComponentRepository());
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

    protected void shutdownContext(AbstractApplicationContext context) {
        context.stop();
        context.close();
    }

}
