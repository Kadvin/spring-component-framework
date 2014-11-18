/**
 * Developer: Kadvin Date: 14/11/12 下午1:28
 */
package sun.net.www.protocol.component;

import net.happyonroad.component.core.support.ComponentURLStreamHandlerFactory;

import java.io.*;
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

        public ComponentURLConnection(URL u) {
            super(u);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            File componentFile = ComponentURLStreamHandlerFactory.getFactory().getMappingFile(this.url);
            try {
                return new FileInputStream(componentFile);
            } catch (FileNotFoundException e) {
                throw e;
            }
        }


    }
}
