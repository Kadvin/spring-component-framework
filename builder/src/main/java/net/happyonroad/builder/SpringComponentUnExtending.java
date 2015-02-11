/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/** The spring component cleaner*/
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "un_extend", inheritByDefault = true, defaultPhase = LifecyclePhase.CLEAN)
public class SpringComponentUnExtending extends AbstractMojo {
    /**
     * POM
     */
    @Component
    protected MavenProject project;
    @Parameter
    File target;
    // 扩展包存放的位置，默认是 repository
    @Parameter
    String extensionPath = "repository";

    public void execute() throws MojoExecutionException {
        File artifact = artifactFile();
        getLog().info("Hello, I'm un extending " + artifact.getPath());
        FileUtils.deleteQuietly(artifact);
    }

    private File artifactFile() {
        if (project.getPackaging().equalsIgnoreCase("pom")) {
            String artifactName = project.getGroupId() + "/" + project.getArtifactId() +
                                  "@" + project.getVersion() + "." + project.getPackaging();
            return new File(target, extensionPath + "/" + artifactName);
        } else {
            String artifactName = project.getGroupId() + "/" + project.getArtifactId() +
                                  "@" + project.getVersion()  + "." + project.getPackaging() ;
            return new File(target, extensionPath + "/" + artifactName);
        }
    }
}
