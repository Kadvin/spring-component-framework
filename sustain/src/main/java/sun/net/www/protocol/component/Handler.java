/**
 * Developer: Kadvin Date: 14/11/12 下午1:28
 */
package sun.net.www.protocol.component;

import net.happyonroad.component.container.support.DefaultComponentRepository;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.InvalidComponentNameException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A url handler to handle url like: component://dnt.components.config-0.1.9.jar
 */
@SuppressWarnings("UnusedDeclaration")
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new ComponentURLConnection(u);
    }

    private static class ComponentURLConnection extends URLConnection {

        private DefaultComponentRepository repository;

        public ComponentURLConnection(URL u) {
            super(u);
        }

        @Override
        public void connect() throws IOException {
            if( repository == null ) repository = DefaultComponentRepository.getRepository();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                File componentFile = repository.componentFile(url.getHost());
                return new FileInputStream(componentFile);
            } catch (InvalidComponentNameException e) {
                throw new IOException(e);
            }
        }
    }
}
