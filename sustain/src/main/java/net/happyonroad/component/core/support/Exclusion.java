/**
 * @author XiongJie, Date: 13-9-4
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.Versionize;

/**
 * Exclusion in pom.xml
 */
public class Exclusion {
    private String groupId;

    private String artifactId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public boolean cover(Versionize componentOrDependency) {
        return getGroupId().equals(componentOrDependency.getGroupId()) &&
                getArtifactId().equals(componentOrDependency.getArtifactId());
    }
}
