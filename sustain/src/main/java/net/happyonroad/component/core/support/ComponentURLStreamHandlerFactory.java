/**
 * Developer: Kadvin Date: 14/11/18 下午3:27
 */
package net.happyonroad.component.core.support;

import sun.net.www.protocol.component.Handler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * The component url stream handler factory
 */
public class ComponentURLStreamHandlerFactory implements URLStreamHandlerFactory{
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
        String fileName = url.getFile();
        if( !fileName.endsWith(".jar") ) fileName = fileName + ".jar";
        File componentFile = componentFiles.get(fileName);
        if( componentFile == null )
        {
            // 当第一次建立的快速索引中没有相应文件时
            // 尝试看lib下后来有没有相应的文件
            componentFile = guessFile(url, "lib");
            if( componentFile == null ) componentFile = guessFile(url, "repository");
            if( componentFile == null ) componentFile = guessFile(url, "repository/lib");
            if( componentFile == null ) componentFile = guessFile(url, "boot");
            if( componentFile == null )
                throw new IOException("there is no component named as " + fileName);
        }
        return componentFile;
    }

    protected Map<String, File> initFastIndexes() {
        componentFiles = new HashMap<String, File>();
        String home = getAppHome();
        File[] libFiles = new File(home, "lib").listFiles();
        if (libFiles != null)
            for (File libFile : libFiles) {
                if (libFile.isFile() && libFile.getName().endsWith(".jar")) {
                    componentFiles.put(libFile.getName(), libFile);
                }
            }
        File[] repositoryFiles = new File(home, "repository").listFiles();
        if (repositoryFiles != null)
            for (File repositoryFile : repositoryFiles) {
                if (repositoryFile.isFile() && repositoryFile.getName().endsWith(".jar")) {
                    componentFiles.put(repositoryFile.getName(), repositoryFile);
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

}
