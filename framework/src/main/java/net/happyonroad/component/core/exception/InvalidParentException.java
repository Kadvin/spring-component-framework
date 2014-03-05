/**
 * @author XiongJie, Date: 13-9-16
 */
package net.happyonroad.component.core.exception;

/**
 * 父组件错误
 */
public class InvalidParentException extends InvalidComponentException{
    public InvalidParentException(String groupId, String artifactId, String version, String type, String message) {
        super(groupId, artifactId, version, type, message);
    }
}
