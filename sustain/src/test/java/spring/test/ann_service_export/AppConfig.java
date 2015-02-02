/**
 * Developer: Kadvin Date: 14-5-28 下午2:52
 */
package spring.test.ann_service_export;

import net.happyonroad.component.container.ServiceExporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import spring.test.ann_service_export.beans.AnnotationProvider;
import spring.test.ann_service_export.scan.ScannedProvider;

/**
 * 基于Annotation的组件配置，服务导出
 */
@Configuration
@ComponentScan("spring.test.ann_service_export.scan")
public class AppConfig implements InitializingBean{
    @Autowired
    private ServiceExporter exporter;

    @Bean
    AnnotationService scannedProvider(){
        return new ScannedProvider();
    }

    @Bean
    public AnnotationService annotationService(){
        return new AnnotationProvider();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        exporter.exports(AnnotationService.class, annotationService(), "default");
        exporter.exports(AnnotationService.class, scannedProvider(), "scanned");

    }
}
