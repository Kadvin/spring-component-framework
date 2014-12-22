/**
 * Developer: Kadvin Date: 14-6-20 下午1:08
 */
package net.happyonroad.spring.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * The default Application Configuration
 */
@Configuration
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
@ComponentScan("dnt.*.support")
public class DefaultAppConfig {

    @Bean
    @Qualifier("default")
    public ResourceBundleMessageSource messageSource(){
        return new ResourceBundleMessageSource();
    }

    // 为了支持 @PropertySource + @Value 联合工作，必须有这个对象
    @Bean
    public static PropertySourcesPlaceholderConfigurer pspConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }
}
