/**
 * @author XiongJie, Date: 13-9-2
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.ComponentResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * 组件以目录形式， 未封闭状态下被运行
 */
@SuppressWarnings("UnusedDeclaration")
public class ComponentFolderResource extends ComponentResource {
    protected File folder;

    public ComponentFolderResource(String groupId,
                                   String artifactId,
                                   String version,
                                   File folder) {
        super(groupId, artifactId, version, folder.getName());
        this.folder = folder;
        if( !this.folder.exists() )
            throw new IllegalArgumentException("The component folder: " + folder + " is not exist!");
        try{
            this.manifest = new Manifest(getInputStream("META-INF/MANIFEST.MF"));
        }catch (IOException ioe){
            // NO manifest, create a default
            this.manifest = new Manifest();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 重载父类方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void close() {
        this.manifest.clear();
        this.manifest = null;
    }

    @Override
    public URL getURL() throws MalformedURLException {
        return this.folder.toURI().toURL();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 内部实现方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public InputStream getInputStream(String relativePath) throws IOException {
        File file = new File(folder, relativePath);
        if( !file.exists() )                      {
            String msg = String.format("Can't find any file with relative path = `%s` in folder: `%s`",
                                       relativePath, folder.getAbsolutePath());
            throw new IllegalArgumentException(msg);
        }
        return new FileInputStream(file);
    }

    @Override
    public boolean exists(String relativePath) {
        File file = new File(folder, relativePath);
        return file.exists();
    }

    @Override
    public Resource[] getLocalResourcesUnder(String path) {
        File target = new File(folder, path);
        IOFileFilter all = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return true;
            }

            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        };
        if(target.isDirectory()){
            Collection<File> files = FileUtils.listFiles(target, all, all);
            Set<Resource> resources = new HashSet<Resource>(files.size());
            for (File file : files) {
                resources.add(new FileSystemResource(file));
            }
            return resources.toArray(new Resource[resources.size()]);
        }else{
            return new Resource[]{new FileSystemResource(target)};
        }
    }

    @Override
    public Resource getLocalResourceUnder(String path) {
        File target = new File(folder, path);
        return new FileSystemResource(target);
    }

    @Override
    public URL getLocalResource(String path) {
        try {
            return new File(folder, path).toURI().toURL();
        } catch (MalformedURLException e){
            throw new IllegalArgumentException("Can't create relative url by " + path);
        }
    }
}
