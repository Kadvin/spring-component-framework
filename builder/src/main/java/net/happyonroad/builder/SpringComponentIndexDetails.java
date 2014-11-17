/**
 * Developer: Kadvin Date: 14/11/8 上午7:36
 */
package net.happyonroad.builder;

/**
 * Generate detail information help speedup system load
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromDependencies.AbstractFromDependenciesMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.filters.DestFileFilter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 为基于Spring Component 框架的项目的扩展jar输出文件明细，用于加速系统类加载
 */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "detail", inheritByDefault = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class SpringComponentIndexDetails extends AbstractFromDependenciesMojo {

    @Override
    protected ArtifactsFilter getMarkedArtifactFilter() {
        return new DestFileFilter( this.overWriteReleases, this.overWriteSnapshots, this.overWriteIfNewer,
                                   this.useSubDirectoryPerArtifact, this.useSubDirectoryPerType,
                                   this.useSubDirectoryPerScope, this.useRepositoryLayout, this.stripVersion,
                                   this.outputDirectory );
    }

    @Override
    public void doExecute() throws MojoExecutionException {
        if( project.getPackaging().equals("pom") ) return;
        File classesFolder = new File(project.getBasedir(), "target/classes");
        //this project without src/target
        if( !classesFolder.exists() ) return;
        indexDetails(classesFolder);
        indexDependencies(classesFolder);
    }

    void indexDetails(File classesFolder) throws MojoExecutionException {
        Collection<File> details = FileUtils.listFiles(classesFolder, null, true);
        List<String> lines = new ArrayList<String>(details.size());
        for (File detail : details) {
            String line = StringUtils.substringAfter(detail.getAbsolutePath(),
                                                     classesFolder.getAbsolutePath() + File.separator);
            lines.add(line);
        }
        File detailFile = new File(classesFolder, "META-INF/INDEX.DETAIL");
        writeLines(lines, detailFile);
    }

    void indexDependencies(File classesFolder) throws MojoExecutionException {
        Collection<String> scope = new HashSet<String>();
        scope.add("compile");
        scope.add("runtime");
        getProject().setArtifactFilter(new CumulativeScopeArtifactFilter(scope));
        DependencyStatusSets dss = getDependencySets( this.failOnMissingClassifierArtifact, true );
        Set<Artifact> artifacts = dss.getResolvedDependencies();
        List<String> lines = new ArrayList<String>(artifacts.size());
        for (Artifact artifact : artifacts) {
            if( artifact.getType().equals("pom") ) continue;
            String line = String.format("%s.%s-%s.%s",
                                        artifact.getGroupId() , artifact.getArtifactId() ,
                                        artifact.getVersion() , artifact.getType());
            lines.add(line);
        }
        File dependenciesFile = new File(classesFolder, "META-INF/INDEX.DEPENDS");
        writeLines(lines, dependenciesFile);

    }

    private void writeLines(List<String> lines, File dest) throws MojoExecutionException {
        FileOutputStream fos = null;
        try {
            FileUtils.forceMkdir(dest.getParentFile());
            fos = new FileOutputStream(dest);
            IOUtils.writeLines(lines, "\n", fos);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Can't generate "+dest.getName()+" for " + project.getArtifactId(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't generate "+dest.getName()+" for " + project.getArtifactId(), e);
        } finally {
            IOUtils.closeQuietly(fos);
        }

    }
}
