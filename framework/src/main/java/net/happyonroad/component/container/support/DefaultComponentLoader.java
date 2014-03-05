/**
 * @author XiongJie, Date: 13-9-22
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.classworld.PomClassWorld;
import net.happyonroad.component.container.ComponentLoader;
import net.happyonroad.component.container.ComponentRepository;
import net.happyonroad.component.container.ServiceRegistry;
import net.happyonroad.component.container.feature.ApplicationFeatureResolver;
import net.happyonroad.component.container.feature.ServiceFeatureResolver;
import net.happyonroad.component.container.feature.StaticFeatureResolver;
import net.happyonroad.component.core.*;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 组件加载 */
public class DefaultComponentLoader implements ComponentLoader, ComponentContext {
    private Logger logger = LoggerFactory.getLogger(DefaultComponentLoader.class);

    /*package*/ final PomClassWorld            world;
    /*package*/ final List<FeatureResolver>    featureResolvers;
    /*package*/ final Map<Component, Features> loadedFeatures;
    /*package*/ final ServiceRegistry          registry;
    /*package*/ final ComponentRepository      repository;

    public DefaultComponentLoader(ComponentRepository repository,
                                  PomClassWorld world,
                                  FeatureResolver... resolvers) {
        this.world = world;
        this.repository = repository;
        loadedFeatures = new ConcurrentHashMap<Component, Features>();
        registry = new DefaultServiceRegistry();
        featureResolvers = new LinkedList<FeatureResolver>();
        //注册缺省的特性解析器
        registerResolver(new StaticFeatureResolver().bind(this));
        registerResolver(new ApplicationFeatureResolver().bind(this));
        registerResolver(new ServiceFeatureResolver().bind(this));
        //注册构造时传入的扩展Feature Resolvers
        for (FeatureResolver resolver : resolvers) {
            registerResolver(resolver.bind(this));
        }
    }

    /**
     * 注册一个扩展特性解析器
     *
     * @param resolver 解析器
     */
    public void registerResolver(FeatureResolver resolver) {
        featureResolvers.add(resolver.bind(this));
        Collections.sort(featureResolvers);
    }

    @Override
    public <T extends FeatureResolver> T getFeatureResolver(String name) {
        for (FeatureResolver resolver : featureResolvers) {
            if (resolver.getName().equals(name))
                //noinspection unchecked
                return (T) resolver;
        }
        return null;
    }

    @Override
    public boolean isLoaded(Component component) {
        return loadedFeatures.containsKey(component);
    }

    @Override
    public <T> T getFeature(Component component, String name) {
        Features features = loadedFeatures.get(component);
        if (features == null) return null;
        return features.getFeature(name);
    }

    @Override
    public ClassRealm getLibraryFeature(Component component) {
        Features features = loadedFeatures.get(component);
        if (features == null) return null;
        return features.getLibraryFeature();
    }

    @Override
    public ApplicationContext getApplicationFeature(Component component) {
        Features features = loadedFeatures.get(component);
        if (features == null) return null;
        return features.getApplicationFeature();
    }

    @Override
    public ApplicationContext getServiceFeature(Component component) {
        Features features = loadedFeatures.get(component);
        if (features == null) return null;
        return features.getServiceFeature();
    }

    @Override
    public ClassRealm getClassRealm(String componentId) {
        return world.getClassRealm(componentId);
    }

    @Override
    public ServiceRegistry getRegistry() {
        return registry;
    }

    @Override
    public ComponentLoader getComponentLoader() {
        return this;
    }

    @Override
    public ComponentRepository getComponentRepository() {
        return repository;
    }

    @Override
    public PomClassWorld getWorld() {
        return world;
    }

    /**
     * 自顶向下的加载组件
     *
     * @param component 被加载的组件
     */
    @Override
    public void load(Component component) throws IOException {
        if (isLoaded(component))
            return;
        logger.debug("Loading {}", component);
        //首先加载父组件
        if (component.getParent() != null) {
            //上面会自动检测是否已经加载，防止重复加载
            load(component.getParent());
        }
        //而后加载依赖的组件
        for (Component depended : component.getDependedComponents()) {
            //上面会自动检测是否已经加载，防止重复加载
            load(depended);
        }
        loadSingle(component);
        //最后，加载自身
        logger.debug("Loaded  {}", component);
    }

    /**
     * 剥洋葱一样的卸载组件
     *
     * @param component 被卸载的组件
     */
    @Override
    public void unload(Component component) {
        if (!isLoaded(component)) {
            return;
        }
        logger.debug("Unloading {}", component);
        //先卸载自身
        unloadSingle(component);
        //再卸载依赖
        for (Component depended : component.getDependedComponents()) {
            //上面会自动检测是否已经加载，防止重复加载
            unload(depended);
        }
        //最后卸载其父组件
        if (component.getParent() != null) {
            //上面会自动检测是否已经加载，防止重复加载
            unload(component.getParent());
        }
        logger.debug("Unloaded  {}", component);
    }

    @Override
    public void registerFeature(Component component, String name, Object feature) {
        Features features = loadedFeatures.get(component);
        if (features == null) {
            features = new Features(name, feature);
            loadedFeatures.put(component, features);
        } else {
            features.setFeature(name, feature);
        }
    }

    @Override
    public Object removeFeature(Component component, String name) {
        Features features = loadedFeatures.get(component);
        if (features != null) {
            return features.remove(name);
        }
        return null;
    }

    /**
     * 实际卸载一个组件，不考虑依赖情况
     *
     * @param component 被加载的组件
     * @throws IOException 加载过程中的IO错误
     */
    protected void loadSingle(Component component) throws IOException {
        if (component.isAggregating()) {
            logger.trace("Needn't real load aggregating component {}", component);
            loadedFeatures.put(component, FeatureResolver.AggregatingFlag);
            return;
        }
        ComponentResource resource = component.getResource();
        if (resource == null) {
            throw new IOException("The component " + component + " without resource");
        }
        logger.trace("Actual loading {}", component);
        for (FeatureResolver featureResolver : featureResolvers) {
            if (featureResolver.hasFeature(component)) {
                featureResolver.resolve(component);
            }
        }
    }

    /**
     * 实际卸载一个组件
     *
     * @param component 被卸载的组件
     */
    protected void unloadSingle(Component component) {
        if (component.isAggregating()) {
            logger.trace("Remove aggregating component {}", component);
            loadedFeatures.remove(component);
            return;
        }
        logger.trace("Actual unloading {}", component);
        for (int i = featureResolvers.size() - 1; i >= 0; i--) {
            FeatureResolver resolver = featureResolvers.get(i);
            if (resolver.hasFeature(component)) {
                resolver.release(component);
            }
        }
        loadedFeatures.remove(component);
    }
}
