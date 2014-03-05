/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.exception;

import net.happyonroad.component.core.ComponentException;

/**
 * 组件名称错误
 */
public class InvalidComponentNameException extends ComponentException {
    public InvalidComponentNameException(String s) {
        super(s);
    }
}
