/**
 * Developer: Kadvin Date: 14-6-11 下午4:20
 */
package net.happyonroad.spring;

import net.happyonroad.component.container.ComponentLoader;
import net.happyonroad.component.container.ComponentRepository;
import net.happyonroad.component.container.support.DefaultComponentLoader;
import net.happyonroad.component.container.support.DefaultComponentRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对被用组件加载的模块进行单元测试时的组件环境的Mock
 */
@Configuration
public class MockComponentWorldConfig {

    @Bean
    public ComponentRepository testComponentRepository(){
        String home = System.getProperty("app.home", "temp");
        if( "temp".equals(home) ){
            home = FileUtils.getTempDirectoryPath() + "/component";
        }
        return new DefaultComponentRepository(home);
    }

    @Bean
    public ComponentLoader testComponentLoader(){
        return new DefaultComponentLoader(testComponentRepository());
    }
}
