/**
 * Developer: Kadvin Date: 14/11/12 下午2:32
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.classworld.LoadUnit;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentResource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * The component class loader
 */
public class ComponentClassLoader extends URLClassLoader {
    protected Component component;
    static         String             CLASSPATH_THIS_URL_PREFIX = "classpath?:";
    private static Map<String, Class> shortcuts                 = new HashMap<String, Class>();
    Map<String, LoadUnit> stats = new HashMap<String, LoadUnit>();

    public ComponentClassLoader(Component component) {
        super(new URL[]{component.getURL()}, parentClassLoader(component));
        this.component = component;
    }

    protected static ComponentParentClassLoader parentClassLoader(Component component) {
        return component.getType().equals("pom") ? null : new ComponentParentClassLoader(component);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        LoadUnit unit = loading(name);
        if( unit.isMissed() ){
            unit.missing();
            throw new ClassNotFoundException(name);
        }
        Class klass = shortcuts.get(name);
        if( klass != null ) return klass;
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            unit.missing();
            throw e;
        }
    }

    @Override
    public URL getResource(String name) {
        if(name.startsWith(CLASSPATH_THIS_URL_PREFIX)){
            name = name.substring(CLASSPATH_THIS_URL_PREFIX.length());
            ComponentResource resource = component.getResource();
            return resource.contains(name) ? resource.getLocalResource(name) : null;
        }
        return super.getResource(name);
    }

    @Override
    public URL findResource(String name) {
        if( component.getResource().contains(name))
            return component.getResource().getLocalResource(name);
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // In order to covert the spring namespace handlers
        Enumeration<URL> resources = super.getResources(name);
        List<URL> uniqueResources = reduceDuplication(resources);
        return "META-INF/spring.handlers".equals(name) ? arrange(uniqueResources) : Collections.enumeration(uniqueResources);
    }

    private List<URL> reduceDuplication(Enumeration<URL> resources) {
        ArrayList<URL> commons = new ArrayList<URL>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if( commons.contains( url )) continue;
            commons.add(url);
        }
        return commons;
    }

    private Enumeration<URL> arrange(List<URL> resources){
        URL special = null;
        Iterator<URL> it = resources.iterator();
        while (it.hasNext()) {
            URL url = it.next();
            String path = url.toString();
            if( path.contains("spring-component-framework") || path.contains(ideCpFeature)){
                special = url;
                it.remove();
                break;
            }
        }
        if( special != null ) resources.add(0, special);
        return Collections.enumeration(resources);
    }

    @Override
    public String toString() {
        return "ComponentClassLoader(" + component +')';
    }

    static {
        shortcuts.put("int", int.class);
        shortcuts.put("long", long.class);
        shortcuts.put("short", short.class);
        shortcuts.put("byte", byte.class);
        shortcuts.put("double", double.class);
        shortcuts.put("float", float.class);
        shortcuts.put("boolean", boolean.class);
        shortcuts.put("char", char.class);
        shortcuts.put("void", void.class);
    }

    // ------------------------------------------------------------
    //     统计支持
    // ------------------------------------------------------------

    LoadUnit loading(String className) {
        LoadUnit unit = stats.get(className);
        if (unit == null) {
            unit = new LoadUnit();
            stats.put(className, unit);
        }
        unit.loading();
        return unit;
    }

    static String ideCpFeature = "sustain" + File.separator + "target" + File.separator + "classes" + File.separator;

}
