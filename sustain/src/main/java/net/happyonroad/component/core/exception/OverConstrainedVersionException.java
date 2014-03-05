/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.exception;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentException;

/**
 * 当VersionRange无法匹配出目标版本时抛出该异常
 * 从Maven中copy过来
 */
public class OverConstrainedVersionException extends ComponentException {
    public OverConstrainedVersionException(String s, Component component) {
        super(s);
        this.component = component;
    }
}
