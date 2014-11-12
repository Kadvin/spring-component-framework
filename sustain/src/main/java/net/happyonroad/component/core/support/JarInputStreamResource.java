/**
 * Developer: Kadvin Date: 14/11/12 上午11:29
 */
package net.happyonroad.component.core.support;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A InputStream Resource support url
 */
public class JarInputStreamResource extends InputStreamResource {
    private final URL url;

    public JarInputStreamResource(String url, InputStream inputStream) throws MalformedURLException {
        super(inputStream);
        this.url = new URL(url);
    }

    @Override
    public URL getURL() throws IOException {
        return url;
    }
}
