/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.exception;

/**
 * 组件错误异常
 */
public class InvalidComponentException extends RuntimeException {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String baseMessage;

    public InvalidComponentException(String groupId, String artifactId, String version, String type, String message) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.baseMessage = message;
    }
    public String getMessage()
    {
        return "For artifact {" + getArtifactKey() + "}: " + getBaseMessage();
    }

    public String getBaseMessage()
    {
        return baseMessage;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getType()
    {
        return type;
    }

    public String getVersion()
    {
        return version;
    }

    public String getArtifactKey()
    {
        return groupId + ":" + artifactId + ":" + version + ":" + type;
    }
}
