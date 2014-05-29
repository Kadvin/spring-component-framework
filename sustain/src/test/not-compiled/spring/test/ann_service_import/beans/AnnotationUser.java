/**
 * Developer: Kadvin Date: 14-5-28 下午3:01
 */
package spring.test.ann_service_import.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import spring.test.ann_service_export.AnnotationService;

/**
 * A annotation service user for annotation driven context
 */
public class AnnotationUser implements Lifecycle{
    private boolean running;

    @Autowired
    @Qualifier("annotationProvider")
    private AnnotationService service;

    @Override
    public void start() {
        System.out.println("AnnotationUser started");
        service.helloWorld();
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("AnnotationUser stopped");
        service.goodbyWorld();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
