/**
 * @author XiongJie, Date: 13-9-2
 */
package net.happyonroad.component.core;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

/**
 * 组件的资源
 */
public abstract class ComponentResource {
    protected final String groupId, artifactId;
    private final String   version;
    private final String   fileName;
    protected     Manifest manifest;
    //META-INF目录下INDEX.DETAIL文件的内容
    // 如果没有该文件，或者该文件内容为空，则该对象为空数组
    // 如果还没有初始化过indexes，则该对象为null
    protected     String[] indexes;
    protected     String[] dependencies;

    protected ComponentResource(String groupId, String artifactId, String version, String fileName) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.fileName = fileName;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 主要对外公开API
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getGroupId(){
        return groupId;
    }

    public String getArtifactId(){
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getPomStream() throws IOException {
        return getInputStream(getPomXmlPath());
    }

    protected String getPomXmlPath() {
        return "META-INF/maven/" + getGroupId() + "/" + getArtifactId() + "/pom.xml";
    }

    public InputStream getApplicationStream() throws IOException {
        return getInputStream("META-INF/application.xml");
    }

    public InputStream getServiceStream() throws IOException {
        return getInputStream("META-INF/service.xml");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 次要对外API
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Manifest getManifest() {
        return manifest;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 内部实现方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public abstract InputStream getInputStream(String relativePath) throws IOException;

    public abstract boolean exists(String relativePath);

    public boolean isPomXmlExists(){
        return exists(getPomXmlPath());
    }

    public abstract void close();

    //获取某个目录下所有的资源
    // 如果传入的是文件名称，则返回该文件对应的资源
    public abstract Resource[] getLocalResourcesUnder(String path);

    public abstract Resource getLocalResourceUnder(String path);

    /**
     * 判断本组件包是否被索引
     *
     * @return 存在META-INF/INDEX.DETAIL 文件即为被索引
     */
    public boolean isIndexed() {
        if( indexes == null ){
            InputStream stream = null;
            try {
                stream = getInputStream("META-INF/INDEX.DETAIL");
                List<String> lines = IOUtils.readLines(stream);
                //是否应该讲indexes hash化？
                indexes = lines.toArray(new String[lines.size()]);
                // to be quick searched
                Arrays.sort(indexes);
            } catch (Exception e) {
                indexes = new String[0];
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return indexes.length > 0 ;
    }

    /**
     * 根据index信息，判断本组件包是否包含特点文件
     * @param path 资源的路径
     * @return 是否存在
     */
    public boolean contains(String path) {
        if( isIndexed() ){
            return Arrays.binarySearch(indexes, path) >= 0;
        }else{
            return getLocalResourceUnder(path) != null;
        }
    }

    /**
     * 获取资源url
     * @param path 资源在组件包里面的路径
     * @return 路径
     */
    public abstract URL getLocalResource(String path);

    public String[] getDependencies(){
        if( dependencies == null ){
            InputStream stream = null;
            try {
                stream = getInputStream("META-INF/INDEX.DEPENDS");
                List<String> lines = IOUtils.readLines(stream);
                //是否应该讲indexes hash化？
                dependencies = lines.toArray(new String[lines.size()]);
            } catch (Exception e) {
                dependencies = new String[0];
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return dependencies;
    }
}
