/**
 * Developer: Kadvin Date: 14/11/12 下午1:28
 */
package sun.net.www.protocol.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * A url handler to handle url like: component:dnt.components.config-0.1.9.jar
 */
@SuppressWarnings("UnusedDeclaration")
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new ComponentURLConnection(u);
    }

    private static class ComponentURLConnection extends URLConnection {
        private Map<String, File> componentFiles;

        public ComponentURLConnection(URL u) {
            super(u);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (componentFiles == null) initFastIndexes();
            File componentFile = mappingFile();
            return new FileInputStream(componentFile);
        }

        protected File mappingFile() throws IOException {
            File componentFile = componentFiles.get(url.getFile());
            if( componentFile == null )
            {
                // 当第一次建立的快速索引中没有相应文件时
                // 尝试看lib下后来有没有相应的文件
                componentFile = guessFile("lib");
                if( componentFile == null ) componentFile = guessFile("repository");
                if( componentFile == null ) componentFile = guessFile("repository/lib");
                if( componentFile == null ) componentFile = guessFile("boot");
                if( componentFile == null )
                    throw new IOException("there is no component named as " + url.getFile());
            }
            return componentFile;
        }

        protected void initFastIndexes() {
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
        }

        protected File guessFile(String folder){
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
}
