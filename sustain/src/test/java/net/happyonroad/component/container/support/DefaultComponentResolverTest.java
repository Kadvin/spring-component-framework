/**
 * @author XiongJie, Date: 13-9-13
 */
package net.happyonroad.component.container.support;

import junit.framework.TestCase;
import net.happyonroad.component.container.ComponentResolver;
import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.ComponentUtils;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.component.core.support.Dependency;
import net.happyonroad.component.core.support.Exclusion;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 对缺省组件解析器的测试
 */
public class DefaultComponentResolverTest extends TestCase {
    private ComponentResolver          resolver;
    private DefaultComponentRepository repository;

    static public void setUpDir() {
        String home = System.getProperty("java.io.tmpdir") + File.separator + "DefaultComponentResolverTest";
        File homePath = new File(home);
        if (homePath.exists()) {
            homePath.deleteOnExit();
        }
    }

    static {
        setUpDir();
    }

    public DefaultComponentResolverTest() {
        String home = System.getProperty("java.io.tmpdir") + File.separator + "DefaultComponentResolverTest";
        repository = new DefaultComponentRepository(home);
        resolver = repository.resolver;
    }

    @Override
    public void setUp() throws Exception {
        String home = System.getProperty("java.io.tmpdir") + File.separator + "DefaultComponentResolverTest";
        System.setProperty("app.home", home);
    }

    // ------------------------------------------------------------
    // 测试 Component resolveComponent(InputStream pomDotXml);
    // ------------------------------------------------------------

    /**
     * 测试目的：
     *   测试对最基本的Pom.xml能不能解析成功
     * 验证方式：
     *   解析出来，并且各个属性值被正确设置
     * @throws Exception
     */
    public void testResolveBasicComponent() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/basic.pom");
        try {
            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("spring", component.getGroupId());
            assertEquals("aggregation", component.getArtifactId());
            assertEquals("0.0.1", component.getVersion());
            assertEquals("pom", component.getType());
            assertEquals("Spring Aggregation", component.getName());
            assertEquals("http://www.happyonroad.net/softs/component", component.getUrl());
            assertEquals("The new technology", component.getDescription());
            //assertEquals(true, component.isRelease());
            assertEquals(false, component.isSnapshot());
        } finally {
            stream.close();
        }
    }

    /**
     * 测试目的：
     *   测试某个设定了parent的pom.xml能不能正确解析
     * 验证方式：
     *   能够解析出来，并且其parent指向正确的引用
     * @throws Exception
     */
    public void testResolveComponentWithParent() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/with_parent.pom");
        try {
            Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
            repository.addComponent(theParent);

            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("infrastructure", component.getArtifactId());
            //验证父对象
            Component parent = component.getParent();
            assertEquals(theParent, parent);
        } finally {
            stream.close();
        }
    }

    /**
     * 测试目的：
     *   测试某个组件从父组件处继承属性（如groupId, version)时，能否正常解析
     * 验证方式：
     *   解析出来的子组件未定义过的group, version属性与父组件一致
     * @throws Exception
     */
    public void testResolveComponentWithInheritance() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/inherit_parent.pom");
        try {
            Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
            repository.addComponent(theParent);

            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals(theParent.getGroupId(), component.getGroupId());
            assertEquals("pdm", component.getArtifactId());
            assertEquals(theParent.getVersion(), component.getVersion());
            //验证父对象
            Component parent = component.getParent();
            assertEquals(theParent, parent);
        } finally {
            stream.close();
        }
    }

    /**
     * 测试目的：
     *  测试当 pom 文件中存在自定义属性时，能否正常解析
     * 验证方式：
     *   能够解析出属性
     * @throws Exception
     */
    public void testResolveComponentWithProperties() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/with_properties.pom");
        try {
            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("spring", component.getGroupId());
            assertEquals("aggregation", component.getArtifactId());
            assertEquals("0.0.1", component.getVersion());

            assertNotNull(component.getProperties());
            assertEquals(1, component.getProperties().size());
            assertEquals("test value", component.getProperties().getProperty("test.property.name"));
        } finally {
            stream.close();
        }
    }

    /**
     * 测试目的：
     *  测试当pom文件中存在Maven表达式时，能否正常解析
     * 验证方式：
     *   能够解析出来，并且其动态表达式值正确
     * @throws Exception
     */
    public void testResolveComponentSupportVariable() throws Exception {
        DefaultComponent parent = new DefaultComponent("spring", "parent", "1.9.9", null, "pom");
        repository.addComponent(parent);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/with_variables.pom");
        try {
            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("spring", component.getGroupId());
            assertEquals("aggregation", component.getArtifactId());
            assertEquals("0.0.1", component.getVersion());

            //验证普通属性中的变量计算
            assertEquals("SPRING Aggregation: 0.0.1/pom", component.getName());
            assertEquals("test value", component.getDescription());

            //验证依赖中的变量计算
            assertEquals(1, component.getDependencies().size());
            Dependency dependency = component.getDependencies().get(0);
            assertTrue(dependency.accept(parent));
        } finally {
            stream.close();
        }
    }

    /**
     * 测试目的：
     *   测试对带有依赖信息的Pom.xml能不能解析成功
     * 验证方式：
     *   解析出来，并且包括相应的依赖信息，以及依赖的缺省值，甚至包括依赖中的排除信息
     * @throws Exception
     */
    public void testResolveComponentDependencies() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/with_dependencies.pom");
        try {
            Component slf4j = new DefaultComponent("org.slf4j", "slf4j-api", "1.6.4", null, "jar");
            repository.addComponent(slf4j);
            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("aggregation", component.getArtifactId());
            assertEquals(1, component.getDependencies().size());

            //验证解析出来的依赖
            Dependency dependency = component.getDependencies().get(0);
            assertEquals("org.slf4j", dependency.getGroupId());
            assertEquals("slf4j-api", dependency.getArtifactId());
            assertEquals("1.6.4", dependency.getVersion());
            //顺便测下依赖的缺省值
            //assertEquals("compile", dependency.getScope());
            //assertEquals("jar", dependency.getType());
            assertNull(dependency.getClassifier());

            //验证依赖的排除信息
            assertEquals(1, dependency.getExclusions().size());
            Exclusion exclusion = dependency.getExclusions().iterator().next();
            assertEquals("org.apache", exclusion.getGroupId());
            assertEquals("log4j", exclusion.getArtifactId());
        } finally {
            stream.close();
        }

    }

    /**
     * 测试目的：
     *   测试某个包括modules的pom.xml能不能正确解析
     * 验证方式：
     *   能够解析出来，并且其modules内包含到其他component的引用
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public void skippedTestResolveComponentModules() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("poms/with_modules.pom");
        try {
            Component mock1 = new DefaultComponent("spring", "infrastructure", "0.0.1", null, "jar");
            Component mock2 = new DefaultComponent("spring", "component-framework", "0.0.1", null, "jar");
            Component mock3 = new DefaultComponent("spring", "pdm", "0.0.1", null, "jar");
            Component mock4 = new DefaultComponent("spring", "samples", "0.0.1", null, "jar");
            repository.addComponent(mock1);
            repository.addComponent(mock2);
            repository.addComponent(mock3);
            repository.addComponent(mock4);

            Component component = resolver.resolveComponent(new Dependency("spring", "aggregation", "0.0.1"), stream);
            assertEquals("aggregation", component.getArtifactId());
            assertEquals(4, component.getModules().size());

            //验证解析出来的依赖
            Component subModule1 = component.getModules().get(0);
            Component subModule2 = component.getModules().get(1);
            Component subModule3 = component.getModules().get(2);
            Component subModule4 = component.getModules().get(3);
            assertEquals(mock1, subModule1);
            assertEquals(mock2, subModule2);
            assertEquals(mock3, subModule3);
            assertEquals(mock4, subModule4);
        } finally {
            stream.close();
        }
    }
    // ------------------------------------------------------------
    // 测试 Component resolveComponent(File jarOrPomFilePath) throws InvalidComponentException
    // ------------------------------------------------------------

    /**
     * 测试目的：
     *   测试根据lib目录下的某个jar文件，直接解析出组件对象
     * 验证方式：
     *   解析出来，并且包括其内部Manifest信息, pom.xml中的信息
     * @throws Exception
     */
    public void testResolveComponentFromJarFile() throws Exception {
        Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
        repository.addComponent(theParent);
        copyPom("spring.aggregation/sample@1.0.0.pom", "spring.aggregation/sample@1.0.0.pom", repository.getHome());
        Component compTechnology = new DefaultComponent("spring", "component-framework", "0.0.1", null, "jar");
        repository.addComponent(compTechnology);
        File jarFile = getResourceFile("jars/spring.aggregation/sample@1.0.0.jar");
        FileUtils.copyFile(jarFile, new File(repository.getHome(), "lib/" + ComponentUtils.relativePath(jarFile)));
        Component component = resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0.jar"),
                                                        new FileSystemResource(jarFile));
        assertEquals("spring.aggregation", component.getGroupId());
        assertEquals("sample", component.getArtifactId());
        assertEquals("1.0.0", component.getVersion());
        assertEquals("jar", component.getType());
        //Pom中独有的信息
        assertEquals("SPRING Aggregation: Sample", component.getName());
        assertEquals(1, component.getDependencies().size());
        //验证父对象
        Component parent = component.getParent();
        assertEquals(theParent, parent);
    }

    /**
     * 测试目的：
     *   测试根据lib/poms目录下的某个pom.xml文件，能否直接解析出组件对象
     * 验证方式：
     *   解析出来，并且包括相应的依赖信息，以及子模块信息，
     * @throws Exception
     */
    public void testResolveComponentFromPomFile() throws Exception {
        Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
        repository.addComponent(theParent);
        Component compTechnology = new DefaultComponent("spring", "component-framework", "0.0.1", null, "jar");
        repository.addComponent(compTechnology);

        File pomFile = getResourceFile("poms/spring.aggregation/sample@1.0.0.pom");
        Component component = resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0.pom"),
                                                        new FileSystemResource(pomFile));
        assertEquals("spring.aggregation", component.getGroupId());
        assertEquals("sample", component.getArtifactId());
        assertEquals("1.0.0", component.getVersion());
        assertEquals("jar", component.getType());
        //Pom中独有的信息
        assertEquals("Spring Aggregation: Sample", component.getName());
        assertEquals(1, component.getDependencies().size());
        //验证父对象
        Component parent = component.getParent();
        assertEquals(theParent, parent);
    }

    // ------------------------------------------------------------
    // 对 Component resolveComponent(InputStream pomDotXml);
    // 的un-happycases，也就是异常cases
    // ------------------------------------------------------------

    /**
     * 测试目的：
     *   测试解析lib目录下某个名字不符合规范的第三方jar包
     * 验证方式：
     *   应该抛出 InvalidComponentException
     * @throws Exception
     */
    public void testResolveComponentFromFileWithInvalidName() throws Exception {
        File jarFile = getResourceFile("jars/invalid.component.jar");
        try {
            resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0"),
                                      new FileSystemResource(jarFile));
            fail("it should raise invalid component name exception");
        } catch (InvalidComponentNameException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * 测试目的：
     *   测试解析lib目录下某个名字虽然符合规范，但pom.xml内部无效（不存在)的第三方jar包
     * 验证方式：
     *   应该抛出 DependencyNotMeetException
     * @throws Exception
     */
//    public void testResolveComponentFromFileWithInvalidContent() throws Exception {
//        File pomFile = getResourceFile("poms/invalid.component.pom");
//        try {
//            resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0"),
//                                      new FileInputStream(pomFile));
//            fail("it should raise invalid component name exception");
//        } catch (DependencyNotMeetException e) {
//            System.out.println(e.getMessage());
//        }
//    }

    /**
     * 测试目的：
     *   测试lib目录下某个jar的名字虽然符合规范，但其中pom.xml内容与名字不匹配
     * 验证方式：
     *   应该抛出 InvalidComponentException
     * @throws Exception
     */
    public void testResolveComponentFromNameContentConflictJar() throws Exception {
        Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
        repository.addComponent(theParent);
        Component compTechnology = new DefaultComponent("spring", "component-framework", "0.0.1", null, "jar");
        repository.addComponent(compTechnology);

        File jarFile = getResourceFile("jars/spring.aggregation/sample-conflict@1.0.0.jar");
        try {
            resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0.jar"),
                                      new FileSystemResource(jarFile));
            fail("it should raise invalid component name exception");
        } catch (InvalidComponentNameException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 测试目的：
     *   测试lib/poms目录下的某个pom的名字虽然符合规范，但其内容与名字不匹配
     * 验证方式：
     *   应该抛出 InvalidComponentException
     * @throws Exception
     */
    public void testResolveComponentFromNameContentConflictPom() throws Exception {
        Component theParent = new DefaultComponent("spring", "aggregation", "0.0.1", null, "pom");
        repository.addComponent(theParent);
        Component compTechnology = new DefaultComponent("spring", "component-framework", "0.0.1", null, "jar");
        repository.addComponent(compTechnology);

        File pomFile = getResourceFile("poms/spring.aggregation/sample@1.0.0-conflict.pom");
        try {
            resolver.resolveComponent(Dependency.parse("spring.aggregation/sample@1.0.0.pom"),
                                      new FileSystemResource(pomFile));
            fail("it should raise invalid component name exception");
        } catch (InvalidComponentNameException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 测试目的：
     *   测试artifactId中包含点号的组件，能被解析出来，并将多余的信息转移到groupId中
     * @throws Exception
     */
    public void testResolveComponentWithAbnormalArtifactId() throws Exception {
        Component theParent = new DefaultComponent("net.java", "jvnet-parent", "3", null, "pom");
        repository.addComponent(theParent);

        File pomFile = getResourceFile("poms/javax.servlet/javax.servlet-api@3.1.0.pom");
        Dependency dependency = Dependency.parse("javax.servlet/javax.servlet-api@3.1.0.pom");
        Component component = resolver.resolveComponent(dependency, new FileSystemResource(pomFile));
        assertEquals("javax.servlet", component.getGroupId());
        assertEquals("javax.servlet-api", component.getArtifactId());
        assertEquals("3.1.0", component.getVersion());
    }

    // ------------------------------------------------------------
    // 测试用例支持方法
    // ------------------------------------------------------------
    protected File getResourceFile(String fileName){
        //这些依赖于解开的class-path的测试用例，在maven集成环境上并不能跑过，因为此时这些资源被打包在jar包里面了
        URL resource = getClass().getClassLoader().getResource(fileName);
        assert resource != null;
        //TODO 判断资源是不是包含在jar包中，如果被包含，则把该资源写到临时目录，而后生成面向临时目录的文件
        return new File(resource.getPath());
    }

    protected static void copyPom(String pomName, String relativePath, String root) throws IOException {
        URL source = DefaultComponentLoaderTest.class.getClassLoader().getResource("poms/" + pomName );
        assert source != null;
        File destination = new File(root, "lib/" + relativePath);
        FileUtils.copyURLToFile(source, destination);
    }

}
