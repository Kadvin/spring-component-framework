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
     * <h2>向该Class Loader中注册多个第三方URL</h2>
     * <pre>
     * 一般而言，我们应该将所有第三方url注册到系统统一的class loader上
     * 在开发阶段，pom依赖时解决相同group/artifact多个版本的冲突问题
     * 而不是在运行阶段来支持同一lib包多个版本的情况
     * （因为即便支持了，也会在不经意间出现Class Class Exception)
     * </pre>
     *
     * @param urls 注册的url集合，重复的注册不成功
     */
    public abstract void addURLs(Set<URL> urls);

    protected void innerAddURL(URL url) {
        super.addURL(url);
    }
}
