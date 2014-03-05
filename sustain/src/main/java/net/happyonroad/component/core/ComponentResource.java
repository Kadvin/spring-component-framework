/**
 * @author XiongJie, Date: 13-9-2
 */
package net.happyonroad.component.core;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * 组件的资源
 */
public abstract class ComponentResource {

    protected Manifest manifest;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 主要对外公开API
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public InputStream getPomStream() throws IOException {
        return getInputStream("META-INF/pom.xml");
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

    public abstract void close();

    //获取某个目录下所有的资源
    // 如果传入的是文件名称，则返回该文件对应的资源
    public abstract Resource[] getLocalResourcesUnder(String path);
}
