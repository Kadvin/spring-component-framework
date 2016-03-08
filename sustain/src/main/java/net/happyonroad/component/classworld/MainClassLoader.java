/**
 * Developer: Kadvin Date: 15/2/12 上午9:09
 */
package net.happyonroad.component.classworld;

import net.happyonroad.component.core.support.ComponentUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import sun.misc.URLClassPath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>主类加载器</h1>
 * 本加载器是spring-component-framework启动的系统的主加载器
 * 其父ClassLoader为系统Bootstrap加载器(也就是spring-component-framework.jar的Class-Path指定的加载器)
 * 继承于 URLClassLoader，其 url 范围为AppLaunch启动时，目标组件的所有依赖包（除开父加载器的Class-Path相关url）
 */
public class MainClassLoader extends ManipulateClassLoader{
    static MainClassLoader          instance;
    //parent class loader可见的url
    final  Set<URL>                 sysUrls;
    //当前class loader可见的url
    final  Set<URL>                 mainUrls;

    private MainClassLoader(ClassLoader parent) {
        super(parent);
        sysUrls = new HashSet<URL>();
        try {
            URL modelsUrl = new File(System.getProperty("app.home"), "config/models").toURI().toURL();
            sysUrls.add(modelsUrl);
        } catch (MalformedURLException e) {
            //skip
        }
        digSysURLs(parent);
        mainUrls = new HashSet<URL>();
    }

    public void addURL(URL url) {
        innerAddURL(url);
    }


    public void addURLs(Set<URL> urls) {
        for (URL url : urls) {
            innerAddURL(url);
        }
    }

    protected void innerAddURL(URL url) {
        if (sysUrls.contains(url))
            return;
        if (!mainUrls.contains(url)) {
            mainUrls.add(url);
            super.innerAddURL(url);
        }
    }

    public Set<URL> getSysUrls() {
        return sysUrls;
    }

    public Set<URL> getMainUrls() {
        return mainUrls;
    }

    public boolean isCover(URL url) {
        return mainUrls.contains(url);
    }

    public static MainClassLoader getInstance() {
        return getInstance(getSystemClassLoader());
    }

    public static MainClassLoader getInstance(ClassLoader classLoader) {
        if( instance == null )
            instance = new MainClassLoader(classLoader);
        return instance;
    }

    void digSysURLs(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            //肯定已经 normalized
            String appHome = System.getProperty("app.home");
            try {
                URLClassPath ucp = (URLClassPath) FieldUtils.readField(classLoader, "ucp", true);
                List loaders = (List) FieldUtils.readField(ucp, "loaders", true);
                for (Object loader : loaders) {
                    try {
                        URL base = (URL) FieldUtils.readField(loader, "csu", true);
                        if( base.getProtocol().equals("file")){
                            String fileName = FilenameUtils.normalize(base.getFile());
                            if(fileName.startsWith(appHome))
                                sysUrls.add(new URL("component:" + ComponentUtils.relativePath(base.getFile())));
                            else
                                sysUrls.add(base);
                        }
                    } catch (Exception ex) {
                        //continue; // can not
                    }
                }
            } catch (Exception ex) {
                //continue; // can not
            }
        }
    }

    @Override
    public String toString() {
        return "MainClassLoader";
    }
}
