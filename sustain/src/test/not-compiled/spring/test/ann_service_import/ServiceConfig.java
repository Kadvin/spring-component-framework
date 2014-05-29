/**
 * Developer: Kadvin Date: 14-5-29 上午9:42
 */
package spring.test.ann_service_import;

import net.happyonroad.spring.service.AbstractServiceConfig;
import spring.test.ann_service_export.AnnotationService;

/**
 * 代替 service.xml 说明有多少service需要export或import
 */
public class ServiceConfig extends AbstractServiceConfig {

    @Override
    public void defineServices() {
        importService(AnnotationService.class.getName(), "default", "annotationProvider");
        importService(AnnotationService.class.getName(), "scanned", "scannedProvider");

        exportService(AnnotationService.class.getName(), "more", "moreAnnotationService");
    }
}
