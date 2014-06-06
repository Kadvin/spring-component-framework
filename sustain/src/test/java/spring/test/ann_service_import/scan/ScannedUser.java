/**
 * Developer: Kadvin Date: 14-5-28 下午3:46
 */
package spring.test.ann_service_import.scan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;
import spring.test.ann_service_export.AnnotationService;

/**
 * ScannedUser
 */
@Component
public class ScannedUser implements Lifecycle {
    private boolean running;

    @Autowired
    @Qualifier("scannedProvider")
    private AnnotationService service;

    @Override
    public void start() {
        System.out.println("ScannedUser started");
        service.helloWorld();
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("ScannedUser stopped");
        service.goodbyWorld();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
