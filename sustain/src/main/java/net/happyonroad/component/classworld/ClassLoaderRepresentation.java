/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;

import net.happyonroad.component.container.AppLauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/** the shared class loader for 3rd library  */
public class ClassLoaderRepresentation extends ClassLoader {

    private static SharedClassLoader sharedClassLoader = new SharedClassLoader(AppLauncher.class.getClassLoader());

    private final Set<URL> urls;

    public ClassLoaderRepresentation(Set<URL> urls) {
        sharedClassLoader.addURLs(urls);
        this.urls = urls;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> theClass = sharedClassLoader.loadClass(name);
        URL hostUrl = sharedClassLoader.hostUrl(theClass);
        if (hostUrl == null) //it's loaded by parent actually, not by the shared instance
            return theClass;
        if(accessible(hostUrl))
            return theClass;
        else
            throw new ClassNotFoundException(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = sharedClassLoader.getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = sharedClassLoader.getResource(name);
        if(accessible(url))
            return url;
        else
            return null;
    }

    //judge the url is accessible from this class representation or not
    private boolean accessible(URL resourceUrl) {
        if(resourceUrl == null)
            return false;
        for (URL url : urls) {
            if( resourceUrl.toString().contains(url.toString()))
                return true;
        }
        return false;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = sharedClassLoader.getResources(name);
        Set<URL> urls = new HashSet<URL>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if(accessible(url)) urls.add(url);
        }
        return Collections.enumeration(urls);
    }

    public static Set<URL> getThirdpartURLs() {
        return sharedClassLoader.allUrls;
    }
}
