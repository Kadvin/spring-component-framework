/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.support;

import net.happyonroad.component.core.Component;
import net.happyonroad.component.core.exception.InvalidComponentNameException;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 组件工具
 * Copy from Maven ArtifactUtils
 */
@SuppressWarnings("unused")
public final class ComponentUtils
{

    private ComponentUtils()
    {
    }

    public static  String relativePath(String filePath) {
        return relativePath(new File(filePath));
    }

    public static  String relativePath(File file) {
        return file.getParentFile().getName() + "/" + file.getName();
    }

    public static boolean isSnapshot( String version )
    {
        return version != null &&
            ( version.toUpperCase().endsWith( Component.SNAPSHOT_VERSION ) || Component.VERSION_FILE_PATTERN.matcher( version )
                .matches() );
    }

    public static String toSnapshotVersion( String version )
    {
        Matcher m = Component.VERSION_FILE_PATTERN.matcher( version );
        if ( m.matches() )
        {
            return m.group( 1 ) + '-' + Component.SNAPSHOT_VERSION;
        }
        else
        {
            return version;
        }
    }

    public static String versionlessKey( Component component)
    {
        return versionlessKey( component.getGroupId(), component.getArtifactId() );
    }

    public static String versionlessKey( String groupId, String componentId)
    {
        if ( groupId == null )
        {
            throw new NullPointerException( "groupId was null" );
        }
        if ( componentId == null )
        {
            throw new NullPointerException( "ComponentId was null" );
        }
        return groupId + ":" + componentId;
    }

    public static String componentId(String groupId, String componentId, String type, String version)
    {
        return componentId(groupId, componentId, type, null, version);
    }

    public static String componentId(String groupId, String componentId, String type, String classifier, String baseVersion)
    {
        return groupId + ":" + componentId + ":" + type + ( classifier != null ? ":" + classifier : "" ) + ":" +
            baseVersion;
    }

    public static Map<String, Component> componentMapByVersionlessId(Collection components)
    {
        Map<String, Component> componentMap = new LinkedHashMap<String, Component>();

        if ( components != null )
        {
            for (Object component : components) {
                Component theComponent = (Component) component;

                componentMap.put(versionlessKey(theComponent), theComponent);
            }
        }

        return componentMap;
    }

    public static Map<String, Component> componentMapByComponentId(Collection components)
    {
        Map<String, Component> componentMap = new LinkedHashMap<String, Component>();

        if ( components != null )
        {
            for (Object component : components) {
                Component theComponent = (Component) component;

                componentMap.put(theComponent.getId(), theComponent);
            }
        }

        return componentMap;
    }

    public static Component copyComponent( Component component)
    {
        DefaultComponent clone = new DefaultComponent( component.getGroupId(), component.getArtifactId(), component.getVersion(),
                                         component.getClassifier(),component.getType());
        clone.setRelease(component.isRelease());
        clone.setResolvedVersion(component.getVersion());
        clone.setUnderlyingResource(component.getUnderlyingResource());

        return clone;
    }
    

    /**
     * 从形如 dnt.pdm.server.adminpane-0.0.1.RELEASE.jar 这种字符串里面解析出组件对象
     * 此时，组件对象的 groupId, artifactId, Version, snapshot, release 等都应该被正确设置
     *
     * @param fileName 包括 groupId, artifactId, version 等组件信息的全限定文件名称
     *             <ul>名称形式：
     *             <li>dnt.pdm.server.adminpane-0.0.1.jar</li>
     *             <li>dnt.pdm.server.adminpane-0.0.1.RELEASE.jar</li>
     *             <li>dnt.pdm.server.adminpane-0.0.1.SNAPSHOT.jar</li>
     *             </ul>
     * @return 悬空的组件实例
     */
    public static Component parseComponent(String fileName) throws InvalidComponentNameException {
        Dependency dependency = Dependency.parse(fileName);
        return new DefaultComponent(dependency);
    }

    private static <T> List<T> copyList( List<T> original )
    {
        List<T> copy = null;
        
        if ( original != null )
        {
            copy = new ArrayList<T>();
            
            if ( !original.isEmpty() )
            {
                copy.addAll( original );
            }
        }
        
        return copy;
    }

}