/**
 * Developer: Kadvin Date: 14-5-28 下午2:52
 */
package spring.test.ann_service_import;

import net.happyonroad.component.container.ServiceExporter;
import net.happyonroad.component.container.ServiceImporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import spring.test.ann_service_export.AnnotationService;
import spring.test.ann_service_import.beans.AnnotationUser;
import spring.test.ann_service_import.more.MoreProvider;

/**
 * 基于Annotation的应用配置，引用服务
 */
@Configuration
@ComponentScan("spring.test.ann_service_import")
public class AppConfig implements InitializingBean{
    @Autowired
    private ServiceImporter importer;
    @Autowired
    private ServiceExporter exporter;

    @Bean
    public AnnotationService moreAnnotationService(){
        return new MoreProvider();
    }

    @Bean
    public AnnotationUser annotationUser() {
        return new AnnotationUser();
    }

    @Bean
    public AnnotationService annotationProvider() {
        return importer.imports(AnnotationService.class);
    }

    @Bean
    public AnnotationService scannedProvider(){
        return importer.imports(AnnotationService.class, "scanned");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        exporter.exports(AnnotationService.class, moreAnnotationService(), "more");
    }
}
