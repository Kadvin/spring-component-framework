/**
 * @author XiongJie, Date: 13-8-29
 */
package net.happyonroad.component.core.exception;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.ComponentException;
import net.happyonroad.component.core.support.Dependency;

/**
 * 组件依赖未满足
 */
public class DependencyNotMeetException extends ComponentException {
    private final Dependency dependency;

    public DependencyNotMeetException(Dependency dependency) {
        this(null, dependency, null);
    }

    public DependencyNotMeetException(Component component, Dependency dependency, Throwable cause) {
        super(dependency + " can't be meet", cause);
        this.dependency = dependency;
        this.component = component;
    }

    public Dependency getDependency() {
        return dependency;
    }

}
