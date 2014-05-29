/**
 * Developer: Kadvin Date: 14-5-29 下午1:16
 */
package net.happyonroad.spring.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * The default Application Configuration
 */
@Configuration
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class DefaultAppConfig {

    @Bean
    public ResourceBundleMessageSource messageSource(){
        return new ResourceBundleMessageSource();
    }
}
