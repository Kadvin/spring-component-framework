/**
 * Developer: Kadvin Date: 14-5-28 下午2:52
 */
package spring.test.ann_service_export;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import spring.test.ann_service_export.beans.AnnotationProvider;

/**
 * 基于Annotation的组件配置，服务导出
 */
@Configuration
@ComponentScan("spring.test.ann_service_export.scan")
public class AppConfig {

    @Bean
    @Qualifier("default")
    public AnnotationService annotationService(){
        return new AnnotationProvider();
    }
}
