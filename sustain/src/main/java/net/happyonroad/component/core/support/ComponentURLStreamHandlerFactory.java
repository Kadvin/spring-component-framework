/**
 * Developer: Kadvin Date: 14/11/18 下午3:27
 */
package net.happyonroad.component.core.support;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import sun.net.www.protocol.component.Handler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The component url stream handler factory
 */
public class ComponentURLStreamHandlerFactory implements URLStreamHandlerFactory, IOFileFilter{
    private static ComponentURLStreamHandlerFactory instance;
    // component id to real file
    private        Map<String, File>                componentFiles;

    public static ComponentURLStreamHandlerFactory getFactory() {
        if (instance == null) instance = new ComponentURLStreamHandlerFactory();
        return instance;
    }

    ComponentURLStreamHandlerFactory() {
        instance = this;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return protocol.equals("component") ? new Handler() : null;
    }

    /**
     * Set the real file of component url
     * @param url the component url
     * @param realFile the real file
     */
    public void setMappingFile(URL url, String realFile){
        if (componentFiles == null) initFastIndexes();
        File file = new File(realFile);
        componentFiles.put(url.getFile(), file);
    }

    /**
     * Get the real file of component url
     * @param url the component url
     * @return the real file
     * @throws IOException
     */
    public File getMappingFile(URL url) throws IOException {
        if (componentFiles == null) initFastIndexes();
        String fileName = FilenameUtils.normalize(url.getFile(), true);
        if( !fileName.endsWith(".jar") ) fileName = fileName + ".jar";
        if( fileName.startsWith("boot/") || fileName.startsWith("boot\\") ){
            fileName = "net.happyonroad/" + FilenameUtils.getName(fileName);
        }else if(fileName.startsWith("lib/") || fileName.startsWith("lib\\")){
            //正常的组件component url里面肯定不是lib开头；
            // 但是 spring-component-framework的Class-Path指定的那些url被normalize之后却会如此
            fileName  = ComponentUtils.relativePath(fileName);
        }
        File componentFile = componentFiles.get(fileName);
        if( componentFile == null )
        {
            // 当第一次建立的快速索引中没有相应文件时
            // 尝试看lib下后来有没有相应的文件
            componentFile = guessFile(url, "lib");
            if( componentFile == null ) componentFile = guessFile(url, "repository");
            if( componentFile == null )
                throw new IOException("there is no component named as " + fileName);
        }
        return componentFile;
    }

    protected Map<String, File> initFastIndexes() {
        componentFiles = new HashMap<String, File>();
        String home = getAppHome();
        File bootFolder = new File(home, "boot");
        if( bootFolder.isDirectory() ){
            Collection<File> frameworkJars = FileUtils.listFiles(bootFolder, this, null);
            for (File frameworkJar : frameworkJars) {
                String relativeName = "net.happyonroad/" + frameworkJar.getName();
                componentFiles.put(relativeName, frameworkJar);
            }
        }
        File libFolder = new File(home, "lib");
        if( libFolder.isDirectory()){
            Collection<File> libFiles = FileUtils.listFiles(libFolder, new String[]{"jar"}, true);
            for (File libFile : libFiles) {
                if (libFile.isFile() && libFile.getName().endsWith(".jar")) {
                    String relativeName = ComponentUtils.relativePath(libFile);
                    componentFiles.put(relativeName, libFile);
                }
            }
        }
        File repositoryFolder = new File(home, "repository");
        if( repositoryFolder.isDirectory() ){
            Collection<File> repositoryFiles = FileUtils.listFiles(repositoryFolder, new String[]{"jar"}, true);
            for (File repositoryFile : repositoryFiles) {
                if (repositoryFile.isFile() && repositoryFile.getName().endsWith(".jar")) {
                    componentFiles.put(ComponentUtils.relativePath(repositoryFile), repositoryFile);
                }
            }
        }
        return componentFiles;
    }


    protected File guessFile(URL url, String folder){
        File file = new File(getAppHome(), folder + "/" + url.getFile());
        if( file.exists() )
        {
            componentFiles.put(url.getFile(), file);
            return file;
        }else{
            return null;
        }
    }

    private String getAppHome() {
        return System.getProperty("app.home", System.getProperty("user.dir"));
    }

    @Override
    public boolean accept(File file) {
        return file.getName().contains("spring-component-framework@");
    }

    @Override
    public boolean accept(File dir, String name) {
        return false;
    }
}
