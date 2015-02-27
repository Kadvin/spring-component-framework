/**
 * Developer: Kadvin Date: 15/2/27 上午11:16
 */
package com.myapp;

import com.myapp.api.CacheService;
import net.happyonroad.spring.config.AbstractAppConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Basis App Config
 */
@Configuration
@ComponentScan("com.myapp.basis")
public class BasisAppConfig extends AbstractAppConfig{
    @Override
    protected void doExports() {
        exports(CacheService.class);
    }
}
