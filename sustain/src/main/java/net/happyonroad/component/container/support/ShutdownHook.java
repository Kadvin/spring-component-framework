/**
 * @author XiongJie, Date: 13-11-25
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.container.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** the shutdown hook */
public class ShutdownHook extends Thread {
    private Executable main;
    private Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    public ShutdownHook(Executable main) {
        this.main = main;
    }

    @Override
    public void run() {
        try {
            logger.info("Try to shutdown gracefully");
            main.exit();
        } catch (Exception ex) {
            logger.warn("Can't stop gracefully while detect the app is shutting down: " + ex.getMessage());
        }
    }
}
