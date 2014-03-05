/**
 * @author XiongJie, Date: 13-9-17
 */
package net.happyonroad.component.core.exception;

/**
 * 资源找不到抛出该异常
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
