/**
 * Developer: Kadvin Date: 14-5-29 下午1:07
 */
package net.happyonroad.component.container.message;

import net.happyonroad.component.ComponentTestSupport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * 测试组件提供资源的功能
 */
public class ComponentMessageSourceTest extends ComponentTestSupport{
    @BeforeClass
    public static void setUpTotal() throws Exception {
        System.setProperty("app.prefix","com.itsnow.;spring.test");
        createPom("comp_0", "spring/test@0.0.1", tempFolder);
        createJar("comp_c", "spring.test/comp_c@0.0.1", "spring/test/api", tempFolder);
        createJar("comp_d", "spring.test/comp_d@0.0.1", "spring/test/api", tempFolder);
    }

    @Test
    public void testProvideSelfDefinedMessageSources() throws Exception{
        target = repository.resolveComponent("spring.test/comp_d@0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);

        String messageD = context.getMessage("message_d", new Object[0], Locale.CHINA);
        Assert.assertEquals("The message defined by comp_d", messageD);

        String messageMore = context.getMessage("message_m", new Object[0], Locale.CHINA);
        Assert.assertEquals("The more messages defined by comp_d", messageMore);
    }

    @Test
    public void testProvideMissingMessageSources() throws Exception{
        target = repository.resolveComponent("spring.test/comp_d@0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);
        try {
            context.getMessage("missing", new Object[0], Locale.CHINA);
            Assert.fail("It should raise NoSuchMessageException");
        } catch (NoSuchMessageException e) {
            //
        }
    }

    @Test
    public void testProvideDependedMessageSources() throws Exception{
        target = repository.resolveComponent("spring.test/comp_d@0.0.1");
        loader.load(target);
        Assert.assertTrue(loader.isLoaded(target));
        ApplicationContext context = loader.getApplicationFeature(target);

        // comp_d cover comp_c's message
        String messageDup = context.getMessage("message_dup", new Object[0], Locale.CHINA);
        Assert.assertEquals("The duplicate message defined by comp_d", messageDup);

        // comp_d delegate comp_c to find message
        String messageC = context.getMessage("message_c", new Object[0], Locale.CHINA);
        Assert.assertEquals("The message defined by comp_c", messageC);
    }


}
