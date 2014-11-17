/**
 * @author XiongJie, Date: 13-11-27
 */
package net.happyonroad.component.classworld;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

/** Test the class loader representation */
public class ClassLoaderRepresentationTest {
    ClassLoaderRepresentation jdomRepresentation, antRepresentation;

    @Before
    public void setUp() throws Exception {
        URL jdomUrl = getClass().getClassLoader().getResource("jars/jdom.jdom-1.0.jar");
        URL antUrl = getClass().getClassLoader().getResource("jars/ant-contrib.ant-contrib-20020829.jar");
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
        Collection<URL> urls = asCollection(en);
        Assert.assertEquals(1, urls.size());

        en = antRepresentation.getResources("META-INF/MANIFEST.MF");
        urls = asCollection(en);
        Assert.assertEquals(1, urls.size());
    }

    protected Collection<URL> asCollection(Enumeration<URL> en){
        Collection<URL> urls = new HashSet<URL>();
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            urls.add(url);
        }
        return urls;
    }
}
