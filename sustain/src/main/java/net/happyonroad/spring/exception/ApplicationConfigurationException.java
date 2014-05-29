/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring.exception;

/**
 * 应用配置错误
 */
public class ApplicationConfigurationException extends RuntimeException {
    public ApplicationConfigurationException(String message) {
        super(message);
    }

    public ApplicationConfigurationException(String s, Exception ex) {
        super(s, ex);
    }
}
