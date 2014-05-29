/**
 * Developer: Kadvin Date: 14-5-28 下午1:37
 */
package net.happyonroad.component.spring;

import net.happyonroad.component.ComponentTestSupport;
import net.happyonroad.spring.context.AnnotationComponentApplicationContext;
import net.happyonroad.spring.context.GenericServiceApplicationContext;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * 测试基于Annotation的spring组件和服务配置
 */
public class AnnotationDrivenTest extends ComponentTestSupport{
    @BeforeClass
    public static void setUpTotal() throws Exception {
        createPom("comp_0", "spring.test-0.0.1", tempFolder);
        if("create".equals(System.getenv("spring.test.action") )){
            createJar("comp_9", "spring.test.comp_9-0.0.1", "spring/test/ann_application", tempFolder);
            createJar("comp_a", "spring.test.comp_a-0.0.1", "spring/test/ann_service_export", tempFolder);
            createJar("comp_b", "spring.test.comp_b-0.0.1", "spring/test/ann_service_import", tempFolder);
        }else{
            copyJar("spring.test.comp_9-0.0.1", tempFolder);
            copyJar("spring.test.comp_a-0.0.1", tempFolder);
            copyJar("spring.test.comp_b-0.0.1", tempFolder);
        }
    }

    @AfterClass
    public static void tearDownTotal() throws Exception {
        try {
            FileUtils.deleteDirectory(tempFolder);
        } catch (IOException e) {
            e.printStackTrace();//TODO resolve it later
        }
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
        context = loader.getServiceFeature(target);
        Assert.assertNotNull(context);
        Assert.assertTrue(context instanceof GenericServiceApplicationContext);
    }

    @Test
    public void testLoadServiceWithImportByAnnotation() throws Exception{
        target = repository.resolveComponent("spring.test.comp_b-0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);
        Assert.assertNotNull(context);
        context = loader.getServiceFeature(target);
        Assert.assertNotNull(context);
        Assert.assertTrue(context instanceof GenericServiceApplicationContext);
    }

}
