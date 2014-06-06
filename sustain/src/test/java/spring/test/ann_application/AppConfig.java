/**
 * Developer: Kadvin Date: 14-5-28 下午2:47
 */
package spring.test.ann_application;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The application configuration for test purpose
 */
@Configuration
@ComponentScan("spring.test.ann_application.beans")
public class AppConfig {
}
