/**
 * Developer: Kadvin Date: 14-5-29 上午9:25
 */
package spring.test.ann_service_import.more;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import spring.test.ann_service_export.AnnotationService;

/**
 * The component been export
 */
public class MoreProvider implements Lifecycle, AnnotationService {
    private boolean           running;

    @Autowired
    @Qualifier("annotationProvider")
    private AnnotationService defaultService;
    @Autowired
    @Qualifier("scannedProvider")
    private AnnotationService scannedService;

    @Override
    public void start() {
        System.out.println("MoreProvider bean started");
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("MoreProvider bean stopped");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void helloWorld() {
        System.out.println("MoreProvider say hi~");
        defaultService.helloWorld();
        scannedService.helloWorld();
    }

    @Override
    public void goodbyWorld() {
        System.out.println("MoreProvider say good bye~");
        defaultService.goodbyWorld();
        scannedService.goodbyWorld();
    }
}