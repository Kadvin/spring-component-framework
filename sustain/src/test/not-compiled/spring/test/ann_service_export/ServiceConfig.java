/**
 * Developer: Kadvin Date: 14-5-29 上午9:42
 */
package spring.test.ann_service_export;

import net.happyonroad.spring.service.AbstractServiceConfig;

/**
 * 代替 service.xml 说明有多少service需要export或import
 */
public class ServiceConfig extends AbstractServiceConfig {

    @Override
    public void defineServices() {
        exportService(AnnotationService.class.getName(), "default", "annotationService");
        exportService(AnnotationService.class.getName(), "scanned", "scannedProvider");
    }
}
