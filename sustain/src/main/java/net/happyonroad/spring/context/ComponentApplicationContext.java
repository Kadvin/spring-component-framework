/**
 * Developer: Kadvin Date: 14-5-28 下午1:08
 */
package net.happyonroad.spring.context;

import net.happyonroad.component.core.Component;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Description here
 */
public interface ComponentApplicationContext extends ConfigurableApplicationContext {
    Component getComponent();
}
