/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.exception;

import net.happyonroad.component.core.ComponentException;

/**
 * 版本错误异常
 */
public class InvalidVersionSpecificationException extends ComponentException {

    public InvalidVersionSpecificationException( String message )
    {
        super( message );
    }
}

