/**
 * Developer: Kadvin Date: 14-5-28 下午2:52
 */
package spring.test.ann_service_import;

import net.happyonroad.component.container.ServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import spring.test.ann_service_export.AnnotationService;
import spring.test.ann_service_import.beans.AnnotationUser;

/**
 * 基于Annotation的应用配置，引用服务
 */
@Configuration
@ComponentScan("spring.test.ann_service_import")
public class AppConfig {
    @Autowired
    private ServiceRegistry serviceRegistry;

    @Bean
    public AnnotationUser annotationUser() {
        return new AnnotationUser();
    }

    @Bean
    public AnnotationService annotationProvider() {
        return serviceRegistry.getService(AnnotationService.class);
    }
}
