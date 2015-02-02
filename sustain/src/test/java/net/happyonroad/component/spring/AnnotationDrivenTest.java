/**
 * Developer: Kadvin Date: 14-5-28 下午1:37
 */
package net.happyonroad.component.spring;

import net.happyonroad.component.ComponentTestSupport;
import net.happyonroad.spring.context.AnnotationComponentApplicationContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * 测试基于Annotation的spring组件和服务配置
 */
public class AnnotationDrivenTest extends ComponentTestSupport{
    @BeforeClass
    public static void setUpTotal() throws Exception {
        System.setProperty("app.prefix","dnt.;spring.test");
        createPom("comp_0", "spring.test-0.0.1", tempFolder);
        createJar("comp_9", "spring.test.comp_9-0.0.1", "spring/test/ann_application", tempFolder);
        createJar("comp_a", "spring.test.comp_a-0.0.1", "spring/test/ann_service_export", tempFolder);
        createJar("comp_b", "spring.test.comp_b-0.0.1", "spring/test/ann_service_import", tempFolder);
    }

    @Test
    public void testLoadApplicationByAnnotation() throws Exception{
        target = repository.resolveComponent("spring.test.comp_9-0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);
        Assert.assertNotNull(context);
        Assert.assertTrue(context instanceof AnnotationComponentApplicationContext);
    }

    @Test
    public void testLoadServiceWithExportByAnnotation() throws Exception{
        target = repository.resolveComponent("spring.test.comp_a-0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);
        Assert.assertNotNull(context);
    }

    @Test
    public void testLoadServiceWithImportByAnnotation() throws Exception{
        target = repository.resolveComponent("spring.test.comp_b-0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);
        Assert.assertNotNull(context);
    }

}
