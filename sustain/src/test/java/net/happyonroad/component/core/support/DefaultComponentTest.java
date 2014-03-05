/**
 * @author XiongJie, Date: 13-9-18
 */
package net.happyonroad.component.core.support;

import junit.framework.TestCase;

import java.util.Properties;

/**
 * 测试 DefaultComponent的一些高级特性
 */
public class DefaultComponentTest extends TestCase {

    private final DefaultComponent component;

    public DefaultComponentTest() {
        component = new DefaultComponent("net.happyonroad.spring", "component", "1.0.0", null, "jar");
        component.setParent(new DefaultComponent("net.happyonroad.spring", "aggregation", "1.0.1", null, "pom"));
        Properties properties = new Properties();
        properties.setProperty("the.property.name", "the.property.value");
        component.setProperties(properties);
    }

    // ------------------------------------------------------------
    // 测试 DefaultComponentTest.resolveVariable(String key)
    // ------------------------------------------------------------


    /**
     * 测试目的：
     *   测试解析组件的属性
     * 验证方式：
     *   getVersion() == resolveVariable("version")
     * @throws Exception
     */
    public void testResolveVariableFromField() throws Exception {
        assertEquals(component.getVersion(), component.resolveVariable("version"));
    }

    /**
     * 测试目的：
     *   测试解析组件的属性
     * 验证方式：
     *   parent.getVersion() == resolveVariable("parent.version")
     * @throws Exception
     */
    public void testResolveVariableFromParent() throws Exception {
        assertEquals(component.getParent().getVersion(), component.resolveVariable("parent.version"));
    }

    /**
     * 测试目的：
     *   测试解析环境变量
     * 验证方式：
     *   System.getenv("java.version") == resolveVariable("java.version")
     * @throws Exception
     */
    public void testResolveVariableFromEnv() throws Exception {
        assertEquals(System.getenv("USERNAME"), component.resolveVariable("env.USERNAME"));
    }

    /**
     * 测试目的：
     *   测试解析环境变量
     * 验证方式：
     *   System.getProperty("java.version") == resolveVariable("java.version")
     * @throws Exception
     */
    public void testResolveVariableFromSystemProperties() throws Exception {
        assertEquals(System.getProperty("java.runtime.name"), component.resolveVariable("java.runtime.name"));
    }

    /**
     * 测试目的：
     *   测试解析组件属性
     * 验证方式：
     *   getProperties("the.property.name") == resolveVariable("the.property.name")
     * @throws Exception
     */
    public void testResolveVariableFromComponentProperties() throws Exception {
        assertEquals("the.property.value", component.resolveVariable("the.property.name"));
    }

    /**
     * 测试目的：
     *   测试解析 project. 开头的组件属性
     * 验证方式：
     *   getVersion() == resolveVariable("version")
     * @throws Exception
     */
    public void testResolveVariableFromProjectProperties() throws Exception {
        assertEquals(component.getVersion(), component.resolveVariable("project.version"));
    }

    /**
     * 测试目的：
     *   测试解析 pom. 开头的组件属性
     * 验证方式：
     *   getVersion() == resolveVariable("version")
     * @throws Exception
     */
    public void testResolveVariableFromPomProperties() throws Exception {
        assertEquals(component.getVersion(), component.resolveVariable("pom.version"));
    }

    //暂时并不支持 build/settings/reporting/scm/dependencyManager 属性之类的
}
