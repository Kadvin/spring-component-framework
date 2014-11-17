/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;

import junit.framework.Assert;
import net.happyonroad.component.ComponentTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

/** Test the class loader representation */
public class ClassLoaderRepresentationTest extends ComponentTestSupport {
    ClassLoaderRepresentation jdomRepresentation, antRepresentation;

    @BeforeClass
    public static void prepareResources()throws Exception{
        URL jdomUrl = ClassLoaderRepresentationTest.class.getClassLoader().getResource("jars/jdom.jdom-1.0.jar");
        assert jdomUrl != null;
        URL antUrl = ClassLoaderRepresentationTest.class.getClassLoader().getResource("jars/ant-contrib.ant-contrib-20020829.jar");
        assert antUrl != null;
        FileUtils.copyURLToFile(jdomUrl, new File(tempFolder, "lib/jdom.jdom-1.0.jar"));
        FileUtils.copyURLToFile(antUrl, new File(tempFolder, "lib/ant-contrib.ant-contrib-20020829.jar"));
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        URL jdomUrl = new URL("component:jdom.jdom-1.0.jar");
        URL antUrl = new URL("component:ant-contrib.ant-contrib-20020829.jar");
        HashSet<URL> jdomSet = new HashSet<URL>();
        jdomSet.add(jdomUrl);
        HashSet<URL> antSet = new HashSet<URL>();
        antSet.add(antUrl);
        jdomRepresentation = new ClassLoaderRepresentation(jdomSet);
        antRepresentation = new ClassLoaderRepresentation(antSet);
    }

    /**
     * <dl>
     * <dt>Purpose:</dt>
     * <dd>The representation can get resource it accessible, can't get resource it without accessibility</dd>
     * <dt>Verification:</dt>
     * <dd>
     * <li>can get resource it accessible
     * <li>can't get resource it without accessibility
     * </dd>
     * </dl>
     *
     * @throws Exception Any Exception
     */
    @Test
    public void testGetResource() throws Exception {
        URL url = jdomRepresentation.getResource("META-INF/info.xml");
        Assert.assertNotNull(url);

        url = jdomRepresentation.getResource("net.sf.antcontrib.logic.antcontrib.properties");
        Assert.assertNull(url);
    }

    /**
     * <dl>
     * <dt>Purpose:</dt>
     * <dd>The representation can load class it accessible, can't load class it without accessibility</dd>
     * <dt>Verification:</dt>
     * <dd>
     * <li>can load class it accessible
     * <li>can't load class it without accessibility
     * </dd>
     * </dl>
     *
     * @throws Exception Any Exception
     */
    @Test
    public void testLoadClass() throws Exception {
        Class<?> about = jdomRepresentation.loadClass("JDOMAbout");
        Assert.assertNotNull(about);
        String origin = System.getProperty("framework.launch", "none");
        System.setProperty("framework.launch", "shell");
        try {
            jdomRepresentation.loadClass("net.sf.antcontrib.logic.ForEach");
            Assert.fail("should raise class not found exception");
        } catch (ClassNotFoundException e) {
            //skip
        } finally {
            System.setProperty("framework.launch", origin);
        }
    }

    /**
     * <dl>
     * <dt>Purpose:</dt>
     * <dd>The representation only get resources it accessible</dd>
     * <dt>Verification:</dt>
     * <dd>
     * <li>only get the resources
     * </dd>
     * </dl>
     *
     * @throws Exception Any Exception
     */
    @Test
    @Ignore("new mechanism make every representation accessible to system cl")
    public void testGetResources() throws Exception {
        Enumeration<URL> en = jdomRepresentation.getResources("META-INF/MANIFEST.MF");
        Collection<URL> urls = Collections.list(en);
        Assert.assertEquals(1, urls.size());

        en = antRepresentation.getResources("META-INF/MANIFEST.MF");
        urls = Collections.list(en);
        Assert.assertEquals(1, urls.size());
    }

}
