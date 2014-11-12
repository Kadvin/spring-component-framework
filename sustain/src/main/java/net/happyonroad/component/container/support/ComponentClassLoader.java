/**
 * Developer: Kadvin Date: 14/11/12 下午2:32
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.core.Component;

import java.net.URL;

/**
 * The component class loader
 */
public class ComponentClassLoader extends ClassLoader {
    protected Component component;

    public ComponentClassLoader(Component component) {
        this.component = component;
    }

    public ComponentClassLoader(Component component, ClassLoader parent) {
        super(parent);
        this.component = component;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    protected URL findResource(String name) {
        if( component.getResource().contains(name))
            return component.getResource().getLocalResource(name);
        return super.findResource(name);
    }
}
