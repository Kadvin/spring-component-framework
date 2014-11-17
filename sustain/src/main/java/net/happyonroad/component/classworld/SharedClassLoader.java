/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import sun.misc.URLClassPath;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 共享的第三方类加载器
 */
class SharedClassLoader extends URLClassLoader {
    final Set<URL> sysUrls;
    final Set<URL> allUrls;

    public SharedClassLoader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
        sysUrls = new HashSet<URL>();
        digSysURLs(classLoader);
        allUrls = new HashSet<URL>();
    }

    protected void digSysURLs(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            try {
                URLClassPath ucp = (URLClassPath) FieldUtils.readField(classLoader, "ucp", true);
                List loaders = (List) FieldUtils.readField(ucp, "loaders", true);
                for (Object loader : loaders) {
                    try {
                        URL base = (URL) FieldUtils.readField(loader, "csu", true);
                        if( base.getProtocol().equals("file")){
                            sysUrls.add(new URL("component:" + FilenameUtils.getName(base.getFile())));
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

    public void addURLs(Set<URL> urls) {
        for (URL url : urls) {
            if (sysUrls.contains(url))
                continue;
            if (!allUrls.contains(url)) {
                allUrls.add(url);
                super.addURL(url);
            }
        }
    }

    // Get the host url of the class loaded by this class
    public URL hostUrl(Class<?> theClass) {
        String path = theClass.getName().replace(".", "/") + ".class";
        URL url = this.findResource(path);
        if (url != null) {
            for (URL jarUrl : allUrls) {
                if (url.getPath().contains(jarUrl.getPath()))
                    return url;
            }
        }
        return null;
    }
}
