/**
 * @author XiongJie, Date: 13-9-18
 */
package net.happyonroad.component.core.support;

import java.util.HashSet;
import java.util.Set;

/**
 * 中间对象，是在我没有很好的XStream转换技巧情况下的过渡方案
 */
public class DependencyManagement {
    // Will be set by xstream
    Set<Dependency> dependencies;


    public Set<Dependency> getDependencies() {
        if (dependencies == null)
            dependencies = new HashSet<Dependency>();
        return dependencies;
    }

    public void merge(DependencyManagement another) {
        if (another == null || another.dependencies == null ) return;
        dependencies.addAll(another.getDependencies());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void unmerge(DependencyManagement another) {
        if( another == null || another.dependencies == null ) return;
        dependencies.removeAll(another.getDependencies());
    }

    public void qualify(Dependency dependency) {
        if( dependencies == null ) return;
        for (Dependency qualified : dependencies) {
            if( qualified.accept(dependency)){
                dependency.exclude(qualified.getExclusions()); return;
            }
        }
    }

    public boolean isEmpty() {
        return dependencies == null || dependencies.isEmpty();
    }
}
