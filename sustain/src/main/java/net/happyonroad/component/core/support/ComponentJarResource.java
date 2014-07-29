/**
 * @author XiongJie, Date: 13-8-28
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.ComponentResource;
import net.happyonroad.component.core.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 组件以Jar包形式，封闭状态运行
 */
public class ComponentJarResource extends ComponentResource {

    protected JarFile file;

    public ComponentJarResource(String groupId, String artifactId, File file) {
        super(groupId, artifactId);
        try {
            this.file = new JarFile(file);
            this.manifest = this.file.getManifest();
        } catch (IOException e) {
            throw new IllegalArgumentException("Bad component jar file: " + file.getPath(), e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 额外扩展的对外方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("UnusedDeclaration")
    public File getFile() {
        return new File(file.getName());
    }

    public void close(){
        this.manifest.clear();
        this.manifest = null;
        try{ this.file.close(); } catch (IOException ex){ /**/ }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 重载父类方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 内部实现方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public InputStream getInputStream(String innerPath) throws IOException {
        JarEntry entry = file.getJarEntry(innerPath);
        if( entry == null )
            throw new ResourceNotFoundException("Can't find " + innerPath + " from " + file.getName());
        return file.getInputStream(entry);
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
                try {
                    UrlResource resource = new UrlResource("jar:file:" + file.getName() +"!/" + entry.getName());
                    matches.add(resource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return matches.toArray(new Resource[matches.size()]);
    }

    @Override
    public Resource getLocalResourceUnder(String path) {
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if(entry.getName().equalsIgnoreCase(path)) {
                try {
                    return new UrlResource("jar:file:" + file.getName() +"!/" + entry.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
