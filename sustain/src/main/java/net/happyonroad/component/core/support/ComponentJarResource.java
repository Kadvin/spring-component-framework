/**
 * @author XiongJie, Date: 13-8-28
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import org.springframework.core.io.Resource;
import sun.net.www.protocol.jar.Handler;
import sun.net.www.protocol.jar.JarURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static net.happyonroad.component.core.support.ComponentUtils.relativePath;

/**
 * 组件以Jar包形式，封闭状态运行
 */
public class ComponentJarResource extends ComponentResource {


    private final JarFile          file;

    public ComponentJarResource(Dependency dependency, String briefId) throws InvalidComponentNameException {
        this(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), briefId);
    }

    public ComponentJarResource(String groupId, String artifactId, String version, String briefId)
            throws InvalidComponentNameException {
        super(groupId, artifactId, version, briefId);
        try {
            URL manifestUrl = new URL(assemble("META-INF/MANIFEST.MF"));
            //借 connection对象生成 JarFile，这个jar file就被系统统一管理；
            // 而后直接返回的 new URL("jar:file:xxx!/yyy") 读取stream时，就会复用这个系统管理的JarFile
            JarURLConnection connection = new JarURLConnection(manifestUrl, new Handler());
            this.file = connection.getJarFile();
            this.manifest = connection.getManifest();
        } catch (IOException e) {
            throw new InvalidComponentNameException("Bad component: " + relativePath(getFileName()), e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 额外扩展的对外方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void close(){
        this.manifest.clear();
        this.manifest = null;
        // system shared jar file, do not close it
        //try{ this.file.close(); } catch (IOException ex){ /**/ }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 重载父类方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public URL getURL() throws MalformedURLException {
        return new URL("component:" + getFileName());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 内部实现方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public InputStream getInputStream(String innerPath) throws IOException {
        JarEntry entry = file.getJarEntry(innerPath);
        if( entry == null )
            throw new IllegalArgumentException("Can't find " + innerPath + " from " + file.getName());
        //class loader可能还没有正确配置好,此时直接从file里面获取
        try {
            return super.getResource(innerPath).getInputStream();
        } catch (IOException e) {
            return file.getInputStream(entry);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            return null != file.getJarEntry(relativePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Resource[] getLocalResourcesUnder(String path) {
        Set<Resource> matches = new HashSet<Resource>();
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if(entry.getName().startsWith(path)) {
                Resource resource = super.getResource(entry.getName());
                matches.add(resource);
            }
        }
        return matches.toArray(new Resource[matches.size()]);
    }

    @Override
    public Resource getLocalResourceUnder(String path) {
        if( isIndexed() && Arrays.binarySearch(indexes, path) >= 0){
            return super.getResource(path);
        }else{
            ZipEntry entry = file.getEntry(path);
            if( entry != null )
                return super.getResource(path);
        }
        return null;
    }

    @Override
    public URL getLocalResource(String path)  {
        final String url = assemble(path);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can't create jar url: " + url);
        }
    }

    // replace file protocol to component
    private String assemble(String path){
        return  "jar:component:" + getFileName() + "!/" + path;
    }
}
