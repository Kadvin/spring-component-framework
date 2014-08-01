/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.Dependency;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromDependencies.CopyDependenciesMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * 对基于Spring Component 框架的项目的扩展部分进行打包
 */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "extend", inheritByDefault = true, defaultPhase = LifecyclePhase.PACKAGE)
public class SpringComponentExtending extends CopyDependenciesMojo {
    //被扩展的发布系统
    @Parameter
    File targetRelease;
    // 扩展包存放的位置，默认是 repository
    @Parameter
    String extensionPath = "repository";

    // 默认认为一般的插件包不会在平台之外引入额外的依赖
    // 如果有引入，要求开发者在相应的模块中，将本插件的该参数设置为true
    // 这样，可以做到既减少copy，又适应扩展的需求
    @Parameter
    boolean copyDependencies = false;

    private File repositoryFile;

    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm extending " + targetRelease.getPath());
        repositoryFile = new File(targetRelease, extensionPath);

        //把 pom 或者 jar copy 到目标系统的扩展目录
        copyTargets();

        if (copyDependencies) {
            //把依赖copy到目标目录
            copyDependencies();

            //清除与平台重复的第三方依赖
            reduceDependencies();
        }

        // 清除与jar重复的pom文件
        reducePoms();
    }


    private void copyTargets() throws MojoExecutionException {
        String fileName = project.getGroupId() + "." + project.getArtifactId() + "-" + project.getVersion();
        try {
            if ("pom".equalsIgnoreCase(project.getPackaging())) {
                //copy main pom
                FileUtils.copyFile(project.getFile(), new File(repositoryFile, "poms/" + fileName + ".pom"));

            } else {
                File newTargetFile = new File(project.getBasedir(), "target/" + fileName + ".jar");
                File defaultTargetFile = new File(project.getBasedir(),
                        "target/" + project.getArtifactId() + "-" + project.getVersion() + ".jar");
                //copy jar
                File output = new File(repositoryFile, fileName + ".jar");
                if (newTargetFile.exists()) {
                    FileUtils.copyFile(newTargetFile, output);
                } else {
                    FileUtils.copyFile(defaultTargetFile, output);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't copy targets: " + e.getMessage());
        }
    }

    private void copyDependencies() throws MojoExecutionException {
        //如果能给指定排除特定依赖引入的其他依赖，就可以少copy许多与平台重复的依赖
        File libPath = new File(repositoryFile, "lib");
        super.setOutputDirectory(libPath);
        super.setCopyPom(true);
        super.setPrependGroupId(true);
        super.addParentPoms = true;
        Collection<String> scope = new HashSet<String>();
        scope.add("test");
        getProject().setArtifactFilter(new CumulativeScopeArtifactFilter(scope));
        super.doExecute();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void reduceDependencies() throws MojoExecutionException {
        File libPath = new File(repositoryFile, "lib");
        if (!libPath.exists()) return;

        File pomsPath = new File(repositoryFile, "poms");
        if (!pomsPath.exists()) pomsPath.mkdirs();

        try {
            List<File> jars = FileUtils.getFiles(libPath, "*.jar", null);
            for (File jar : jars) {
                if (FileUtils.fileExists(targetRelease.getAbsolutePath() + "/lib/" + jar.getName()) ||
                        FileUtils.fileExists(targetRelease.getAbsolutePath() + "/boot/" + jar.getName()) ||
                        FileUtils.fileExists(repositoryFile.getAbsolutePath() + "/" + jar.getName())) {
                    FileUtils.forceDelete(jar);
                }
            }
            List<File> poms = FileUtils.getFiles(libPath, "*.pom", null);
            for (File pom : poms) {
                String jarName = pom.getName().replace(".pom", ".jar");
                if (FileUtils.fileExists(targetRelease.getAbsolutePath() + "/lib/poms/" + pom.getName()) ||
                        FileUtils.fileExists(targetRelease.getAbsolutePath() + "/lib/" + jarName) ||
                        FileUtils.fileExists(targetRelease.getAbsolutePath() + "/boot/" + jarName) ||
                        FileUtils.fileExists(repositoryFile.getAbsolutePath() + "/poms/" + pom.getName()) ||
                        FileUtils.fileExists(repositoryFile.getAbsolutePath() + "/" + jarName)
                        ) {
                    FileUtils.forceDelete(pom);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't reduce depended lib/jars|poms: " + e.getMessage());
        }
    }

    private void reducePoms() throws MojoExecutionException {
        File libPath = new File(repositoryFile, "lib");
        if(!libPath.exists()) return;
        File tempFolder = new File(libPath, "temp");
        try {
            List<File> pomFiles = FileUtils.getFiles(libPath, "*.pom", null);
            for (File pomFile : pomFiles) {
                File newPomFile = new File(repositoryFile, "poms/" + pomFile.getName());
                File jarFile = new File(pomFile.getPath().replace(".pom", ".jar"));
                Dependency dep;
                try {
                    dep = Dependency.parse(pomFile.getName());
                } catch (InvalidComponentNameException e) {
                    throw new MojoExecutionException("Can't parse component from:" + pomFile);
                }
                String pomXmlPath = "META-INF/maven/" + dep.getGroupId() + "/" + dep.getArtifactId();
                if (jarFile.exists()) {
                    JarFile jar = new JarFile(jarFile);
                    ZipEntry mavenFolder = jar.getEntry(pomXmlPath);
                    jar.close();
                    if (mavenFolder != null) {
                        FileUtils.forceDelete(pomFile);
                    } else {
                        File pomFolder = new File(tempFolder, pomXmlPath);
                        FileUtils.forceMkdir(pomFolder);
                        newPomFile = new File(pomFolder, "pom.xml");
                        FileUtils.rename(pomFile, newPomFile);

                        List<String> lines = new ArrayList<String>(3);
                        lines.add("version=" + dep.getVersion());
                        lines.add("groupId=" + dep.getGroupId());
                        lines.add("artifactId=" + dep.getArtifactId());
                        File pomProperties = new File(pomFolder, "pom.properties");
                        boolean created = pomProperties.createNewFile();
                        if (!created) throw new MojoExecutionException("Can't create " + pomProperties.getPath());
                        FileOutputStream pomPropertiesStream = new FileOutputStream(pomProperties, false);
                        try {
                            IOUtils.writeLines(lines, "\n", pomPropertiesStream);
                        } finally {
                            pomPropertiesStream.close();
                        }

                        try {
                            Jar ant = new Jar();
                            ant.setProject(ANT_PROJECT);
                            ant.setUpdate(true);
                            ant.setDestFile(jarFile);
                            FileSet fs = new FileSet();
                            fs.setProject(ANT_PROJECT);
                            fs.setDir(tempFolder);
                            ant.add(fs);
                            ant.execute();
                        } finally {
                            FileUtils.forceDelete(tempFolder);
                        }
                    }
                } else {
                    FileUtils.rename(pomFile, newPomFile);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move/delete poms: " + e.getMessage());
        }

    }

    private static Project ANT_PROJECT = new Project();
}
