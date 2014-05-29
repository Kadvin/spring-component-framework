/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring.exception;

/**
 * 服务配置错误
 */
public class ServiceConfigurationException extends RuntimeException {
    public ServiceConfigurationException(String message) {
        super(message);
    }

    public ServiceConfigurationException(String s, Exception ex) {
        super(s, ex);
    }
}
