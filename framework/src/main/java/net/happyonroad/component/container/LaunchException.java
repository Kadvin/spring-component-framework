/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.container;

/**
 * 运行异常
 */
public class LaunchException extends Exception {
    private int exitCode;

    public LaunchException(String message) {
        this(message, 100);
    }

    public LaunchException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
