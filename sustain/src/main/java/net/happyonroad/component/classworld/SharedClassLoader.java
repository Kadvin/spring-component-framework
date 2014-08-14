/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/** 共享的第三方类加载器 */
class SharedClassLoader extends URLClassLoader{
    final Set<URL> allUrls;

    public SharedClassLoader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
        allUrls = new HashSet<URL>();
    }

    public void addURLs(Set<URL> urls) {
        for (URL url : urls) {
            if(!allUrls.contains(url)){
                allUrls.add(url);
                super.addURL(url);
            }
        }
    }

    // Get the host url of the class loaded by this class
    public URL hostUrl(Class<?> theClass) {
        String path = theClass.getName().replace(".", "/") + ".class";
        URL url = this.findResource(path);
        if( url != null ){
            for (URL jarUrl : allUrls) {
                if(url.getPath().contains(jarUrl.getPath()))
                    return url;
            }
        }
        return null;
    }
}
