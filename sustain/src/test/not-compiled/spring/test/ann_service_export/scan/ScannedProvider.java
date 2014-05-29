/**
 * Developer: Kadvin Date: 14-5-28 下午3:26
 */
package spring.test.ann_service_export.scan;

import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;
import spring.test.ann_service_export.AnnotationService;

/**
 * 基于Scan的bean
 */
@Component
//在组件的类声明上声明该组件被构建之后，还应该被作为服务导出
public class ScannedProvider implements Lifecycle, AnnotationService {
    private boolean running;

    @Override
    public void start() {
        System.out.println("ScannedProvider bean started");
        running = true;
    }

    @Override
    public void stop() {
        System.out.println("ScannedProvider bean stopped");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void helloWorld() {
        System.out.println("ScannedProvider say hi~");
    }

    @Override
    public void goodbyWorld() {
        System.out.println("ScannedProvider say good bye~");
    }
}
