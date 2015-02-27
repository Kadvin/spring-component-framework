/**
 * Developer: Kadvin Date: 15/2/27 下午2:18
 */
package com.myapp;

import com.myapp.api.CacheService;
import net.happyonroad.spring.config.AbstractUserConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The component depends on Basis should import this config
 */
@Configuration
public class BasisUserConfig extends AbstractUserConfig{
    @Bean
    CacheService cacheService(){
        return imports(CacheService.class);
    }
}
