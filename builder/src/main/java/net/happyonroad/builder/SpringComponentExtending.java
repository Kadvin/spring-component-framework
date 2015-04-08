/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import net.happyonroad.component.core.support.DefaultComponent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

import static net.happyonroad.component.core.support.ComponentUtils.relativePath;

/**
 * 对基于Spring Component 框架的项目的扩展部分进行打包
 */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "extend", inheritByDefault = true, defaultPhase = LifecyclePhase.PACKAGE)
public class SpringComponentExtending extends SpringComponentCopyDependencies {
    //被扩展的发布系统
    // 扩展包存放的位置，默认是 repository

    // 默认认为一般的插件包不会在平台之外引入额外的依赖
    // 如果有引入，要求开发者在相应的模块中，将本插件的该参数设置为true
    // 这样，可以做到既减少copy，又适应扩展的需求
    @Parameter
    boolean copyDependencies = false;

    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm extending " + target.getPath());

        //把 pom 或者 jar copy 到目标系统的扩展目录
        File repositoryFolder = getRepositoryFolder();
        copyTargets(repositoryFolder);

        if (copyDependencies) {
            //把依赖copy到目标目录
            copyDependencies(new File(target, "lib"));
        }

        // 清除与jar重复的pom文件
        reducePoms(repositoryFolder);
        //sealPoms(repositoryFolder);

        cleanEmptyFolders(repositoryFolder);
    }

    @Override
    protected void copyFile(File artifact, File destFile) throws MojoExecutionException {
        String relativePath = relativePath(destFile);
        if(DefaultComponent.isApplication(relativePath)){
            destFile = new File(target, "repository/" + relativePath);
        }
        super.copyFile(artifact, destFile);
    }
}
