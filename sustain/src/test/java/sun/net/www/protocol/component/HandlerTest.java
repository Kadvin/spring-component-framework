package sun.net.www.protocol.component;

import net.happyonroad.component.ComponentTestSupport;
import net.happyonroad.component.container.support.DefaultComponentRepository;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.*;

public class HandlerTest extends ComponentTestSupport {

    @BeforeClass
    public static void setUpTotal() throws Exception {
        System.setProperty("app.prefix","dnt.;spring.test");
        System.setProperty("app.home", tempFolder.getAbsolutePath());

        createPom("comp_0", "spring.test-0.0.1", tempFolder);
        createJar("comp_1", "spring.test.comp_1-0.0.1", "spring/test/api", tempFolder);
        createJar("comp_2", "spring.test.comp_2-0.0.1", "spring/test/standalone", tempFolder);
        createJar("comp_3", "spring.test.comp_3-0.0.1", "spring/test/provider", tempFolder);
        createJar("comp_4", "spring.test.comp_4-0.0.1", "spring/test/user", tempFolder);
        createJar("comp_5", "spring.test.comp_5-0.0.1", "spring/test/mixed", tempFolder);
        createJar("comp_6", "spring.test.comp_6-0.0.1", "spring/test/scan_p", tempFolder, new Filter("Provider"));
        createJar("comp_7", "spring.test.comp_7-0.0.1", "spring/test/scan_u", tempFolder, new Filter("User"));
        createJar("comp_8", "spring.test.comp_8-0.0.1", "spring/test/scan_u", tempFolder, new Filter("User"));

    }

    @Test
    public void testComponentProtocol() throws Exception {
        URL url = new URL("component:dnt.components.config-1.2.3.jar");
        assertNotNull(url);
        URLConnection conn = url.openConnection();
        assertNotNull(conn);
    }

    @Test
    public void testOpenStream() throws Exception {
        URL url = new URL("component:spring.test.comp_1-0.0.1.jar");
        assertNotNull(url);

        URLConnection conn = url.openConnection();
        conn.connect();

        InputStream stream = conn.getInputStream();
        assertNotNull(stream);
        stream.close();
    }
}