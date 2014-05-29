/**
 * Developer: Kadvin Date: 14-5-28 下午3:01
 */
package spring.test.ann_application.beans;

import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;

/**
 * A annotation bean for annotation driven context
 */
@Component
public class AnnotationBean implements Lifecycle {
    private boolean running;
    @Override
    public void start() {
        System.out.println("AnnotationBean started");
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("AnnotationBean stopped");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
