/**
 * @author XiongJie, Date: 13-8-29
 */
package net.happyonroad.component.core;

/**
 * 组件异常的基类
 */
public class ComponentException extends Exception{
    protected Component component;

    public ComponentException() {
    }

    public ComponentException(String message) {
        super(message);
    }

    public ComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentException(Throwable cause) {
        super(cause);
    }

    public Component getComponent() {
        return component;
    }
}
