/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;


import net.happyonroad.component.container.AppLauncher;
import net.happyonroad.component.core.support.ComponentURLStreamHandlerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/** the shared class loader for 3rd library  */
public class ClassLoaderRepresentation extends ClassLoader {

    private static SharedClassLoader sharedClassLoader = new SharedClassLoader(AppLauncher.class.getClassLoader());

    static String mavenFeature1 =  "/.m2/repository/";
    static String mavenFeature2 =  "\\.m2\\repository\\";

    private final Set<URL> urls;

    public ClassLoaderRepresentation(Set<URL> urls) {
        sharedClassLoader.addURLs(urls);
        this.urls = urls;
        // all accessible urls
        this.urls.addAll(sharedClassLoader.sysUrls);
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
    protected Class<?> findClass(String name) throws ClassNotFoundException {
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

    private boolean accessible(URL url) {
        if( url == null ) return true;
        URL componentURL = extractComponentURL(url);
        //null 说明这个url没有组件对应，一般是系统url，或者ide的target/classes内的url
        //否则就看对应的组件的url是否在当前representation访问范围之内
        return componentURL == null || urls.contains(componentURL);
    }

    // Convert the URL file part as component
    //  if can't convert, return null
    //  many input forms:
    //  1. jar:file:/path/to/real/file.jar
    //  2. jar:component:group.artifact-version.jar
    //  3. jar:component:path/to/group.artifact-version.jar
    //  4. file:/path/to/resource/file
    private URL replaceFileAsComponentURL(URL resourceUrl) {
        URL url = extractComponentURL(resourceUrl);
        if( url == null ) return null;
        if( resourceUrl.getProtocol().equals("jar")){
            String file = StringUtils.substringBefore(resourceUrl.getFile(), "!/");
            String path = StringUtils.substringAfter(resourceUrl.getFile(), "!/");
            if( file.startsWith("file:")){
                file = StringUtils.substringAfter(file, "file:");
                ComponentURLStreamHandlerFactory factory = ComponentURLStreamHandlerFactory.getFactory();
                factory.setMappingFile(url, file);
            }
            try {
                return new URL("jar:" + url.toString() + "!/" + path);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Can't construct url", e);
            }
        }else{
            return url;
        }
    }

    // Extract the corresponding component url from resource url
    // if no mapping, return null
    private URL extractComponentURL(URL resourceUrl)  {
        try {
            String componentUrl;
            String protocol = resourceUrl.getProtocol();
            if("jar".equals(protocol)){
                String file = StringUtils.substringBefore(resourceUrl.getFile(), "!/");
                componentUrl = convertFileAsComponentUrl(file);
            }else if ( "file".equals(protocol) || "component".equals(protocol)){
                // file | component
                componentUrl = convertFileAsComponentUrl(resourceUrl.toString());
            }else {
                componentUrl = null;
            }
            if( componentUrl == null ) return null;
            return new URL(componentUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Can't construct url", e);
        }
    }

    private String convertFileAsComponentUrl(String file){
        String componentUrl;
        if( file.startsWith("component:") )
            componentUrl = simplify(file);
        else if (file.startsWith("file:"))
            componentUrl = mapping(file);  //component:xxx
        else
            componentUrl = null;
        return componentUrl;
    }

    // 将 component:../path/to/component.jar这种形式的url字符串
    private String simplify(String componentUrlString) {
        String path = StringUtils.substringAfter(componentUrlString , "component:");
        if( path.startsWith("../" ) ){
            String home = System.getProperty("app.home");
            String newPath =
                    FilenameUtils.normalize(new File(home, path).getAbsolutePath());

            return "component:" + newPath.substring(home.length());
        }
        return componentUrlString;
    }

    // 把指向本系统库的url，或者maven库的url转换为指向系统库中的url
    private String mapping(String fileUrlString) {
        String path = StringUtils.substringAfter(fileUrlString , "file:");
        if(path.startsWith(System.getProperty("app.home", System.getProperty("user.dir")))){
            return "component:" + FilenameUtils.getName(path);
        }else {
            if (path.contains(mavenFeature1) || path.contains(mavenFeature2) ){
                String[] segments;
                if( path.contains(mavenFeature1) ){
                    path = StringUtils.substringAfter(path,mavenFeature1);
                    segments = StringUtils.split(path, "/");
                }else{
                    path = StringUtils.substringAfter(path,mavenFeature2);
                    segments = StringUtils.split(path, "\\");
                }
                String artifactAndVersion = segments[segments.length-1];
                String[] groups = new String[segments.length-3];
                System.arraycopy(segments, 0, groups, 0, groups.length);
                String group = StringUtils.join(groups, ".");
                return "component:" + group + "." + artifactAndVersion;
            }else{ // maybe target classes
              return null;
            }
        }

    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = sharedClassLoader.getResources(name);
        Set<URL> urls = new HashSet<URL>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            URL compURL =  replaceFileAsComponentURL(url);
            if( compURL != null ){
                urls.add(compURL);
            }else{
                urls.add(url);
            }
        }
        return Collections.enumeration(urls);
    }
}
