/**
 * Developer: Kadvin Date: 15/2/12 上午9:43
 */
package net.happyonroad.component.classworld;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * 可控的Class Loader
 */
public abstract class ManipulateClassLoader extends URLClassLoader {
    public ManipulateClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
     * 向该Class Loader中注册单个组件的URL
     *
     * @param url 注册的url，如果重复，则不会注册成功
     */
    public abstract void addURL(URL url);

    /**
     * 向该Class Loader中注册多个第三方URL
     *
     * @param urls 注册的url集合，重复的注册不成功
     */
    public abstract void addURLs(Set<URL> urls);

    protected void innerAddURL(URL url){
        super.addURL(url);
    }
}
