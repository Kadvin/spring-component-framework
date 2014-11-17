/**
 * Developer: Kadvin Date: 14/11/14 上午6:57
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.classworld.ClassLoaderRepresentation;
import net.happyonroad.component.core.Component;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * <h1>The parent class loader of a component class loader</h1>
 * it will find class/resource for component in its dependencies
 */
public class ComponentParentClassLoader extends ClassLoader {
    private final List<String>               appBriefIds;
    private       List<ComponentClassLoader> appClassLoaders;
    private final String                     briefId;

    public ComponentParentClassLoader(Component component) {
        super(new ClassLoaderRepresentation(component.getLibURLs()));
        this.briefId = component.getBriefId();
        this.appBriefIds = component.getAppBriefIds();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ComponentClassLoader appCl : getAppClassLoaders()) {
            try {
                Class<?> klass = appCl.loadClass(name);
                if (klass != null) return klass;
            } catch (ClassNotFoundException e) {
                //skip
            }
        }
        return super.findClass(name);
    }

    @Override
    protected URL findResource(String name) {
        for (ComponentClassLoader appCl : getAppClassLoaders()) {
            URL found = appCl.findResource(name);
            if( found != null ) return found;
        }
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Set<URL> founds = new HashSet<URL>();
        for (ComponentClassLoader appCl : getAppClassLoaders()) {
            loadResources(name, founds, appCl);
        }
        loadResources(name, founds,  getParent());
        return Collections.enumeration(founds);
    }

    protected void loadResources(String name, Set<URL> founds, ClassLoader appCl) {
        Enumeration<URL> resources = null;
        try {
            resources = appCl.getResources(name);
        } catch (IOException e) {
            /*continue;*/
        }
        if(resources != null ){
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                founds.add(url);
            }
        }
    }

    protected List<ComponentClassLoader> getAppClassLoaders(){
        if( appClassLoaders == null ){
            appClassLoaders = new ArrayList<ComponentClassLoader>(appBriefIds.size());
            for (String dependency : appBriefIds) {
                try {
                    Component component = DefaultComponentRepository.getRepository().resolveComponent(dependency);
                    appClassLoaders.add(component.getClassLoader());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        return appClassLoaders;
    }

    @Override
    public String toString() {
        return "ComponentParentClassLoader(" + briefId + ')';
    }
}
