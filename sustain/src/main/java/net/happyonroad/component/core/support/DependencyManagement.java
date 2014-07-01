/**
 * @author XiongJie, Date: 13-9-18
 */
package net.happyonroad.component.core.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 中间对象，是在我没有很好的XStream转换技巧情况下的过渡方案
 */
public class DependencyManagement {
    // Will be set by xstream
    List<Dependency> dependencies;


    public List<Dependency> getDependencies() {
        if(dependencies == null )
            dependencies = new ArrayList<Dependency>();
        return dependencies;
    }

    public void merge(DependencyManagement another) {
        if( another == null || another.dependencies == null ) return;
        getDependencies().addAll(another.getDependencies());
    }

    public void unmerge(DependencyManagement another) {
        if( another == null || another.dependencies == null ) return;
        getDependencies().removeAll(another.getDependencies());
    }

    public void qualify(Dependency dependency) {
        if( dependencies == null ) return;
        for (Dependency qualified : dependencies) {
            if( qualified.accept(dependency)){
                dependency.setExclusions(qualified.getExclusions()); return;
            }
        }
    }
}
