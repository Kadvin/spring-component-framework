/**
 * Developer: Kadvin Date: 14-5-28 下午3:01
 */
package spring.test.ann_service_export.beans;

import org.springframework.context.Lifecycle;
import spring.test.ann_service_export.AnnotationService;

/**
 * A annotation service provider for annotation driven context
 */
public class AnnotationProvider implements Lifecycle , AnnotationService{
    private boolean running;

    @Override
    public void start() {
        System.out.println("AnnotationProvider bean started");
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("AnnotationProvider bean stopped");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void helloWorld() {
        System.out.println("AnnotationProvider say hi~");
    }

    @Override
    public void goodbyWorld() {
        System.out.println("AnnotationProvider say good bye~");
    }
}
