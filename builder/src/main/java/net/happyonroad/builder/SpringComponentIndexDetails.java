/**
 * Developer: Kadvin Date: 14/11/8 上午7:36
 */
package net.happyonroad.builder;

/**
 * Description here
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 为基于Spring Component 框架的项目的扩展jar输出文件明细，用于加速系统类加载
 */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "detail", inheritByDefault = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SpringComponentIndexDetails extends AbstractMojo {
    @Component
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( project.getPackaging().equals("pom") ) return;
        File classesFolder = new File(project.getBasedir(), "target/classes");
        Collection<File> details = FileUtils.listFiles(classesFolder, null, true);
        List<String> lines = new ArrayList<String>(details.size());
        for (File detail : details) {
            String line = StringUtils.substringAfter(detail.getAbsolutePath() , classesFolder.getAbsolutePath() + File.separator);
            lines.add(line);
        }
        File detailFile = new File(classesFolder, "META-INF/INDEX.DETAIL");
        FileOutputStream fos = null;
        try {
            FileUtils.forceMkdir(detailFile.getParentFile());
            fos = new FileOutputStream(detailFile);
            IOUtils.writeLines(lines, "\n", fos);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Can't generate INDEX.DETAIL for " + project.getArtifactId(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't generate INDEX.DETAIL for " + project.getArtifactId(), e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }
}
