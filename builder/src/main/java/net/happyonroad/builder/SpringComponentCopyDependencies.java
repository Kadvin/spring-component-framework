/**
 * Developer: Kadvin Date: 15/2/11 上午10:35
 */
package net.happyonroad.builder;

import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.ComponentURLStreamHandlerFactory;
import net.happyonroad.component.core.support.Dependency;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromDependencies.CopyDependenciesMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static net.happyonroad.component.core.support.ComponentUtils.relativePath;

/**
 * Packaging & Extending 's parent
 */
public class SpringComponentCopyDependencies extends CopyDependenciesMojo {
    protected static Project ANT_PROJECT = new Project();

    static {
        try {
            URL.setURLStreamHandlerFactory(ComponentURLStreamHandlerFactory.getFactory());
        } catch (Error e) {
            //skip for duplicate definition
        }
    }

    @Parameter
    String target;

    protected File[] targetFolders;
    private   File repositoryFolder;
    @Parameter(defaultValue = "repository")
    String extensionPath = "repository";

    protected File getTargetFolder(){
        return getTargetFolders()[0];
    }

    protected File[] getTargetFolders(){
        if( targetFolders == null )
        {
            String[] targets = target.split(",|;|\\s+");
            targetFolders = new File[targets.length];
            for (int i = 0; i < targets.length; i++) {
                String target = targets[i];
                targetFolders[i] = new File(target);
            }
        }
        return targetFolders;

    }

    protected File getRepositoryFolder() {
        if (repositoryFolder == null) {
            repositoryFolder = new File(getTargetFolder(), extensionPath);
        }
        return repositoryFolder;
    }

    protected File getRepositoryFolder(File targetFolder) {
        return new File(targetFolder, extensionPath);
    }


    protected void copyTargets(File outputFolder) throws MojoExecutionException {
        String fileName = project.getArtifactId() + "@" + project.getVersion();
        String relativePath = project.getGroupId() + "/" + fileName;
        try {
            if ("pom".equalsIgnoreCase(project.getPackaging())) {
                //copy main pom
                File pomFile = new File(outputFolder, relativePath + ".pom");
                FileUtils.copyFile(project.getFile(), pomFile);
            } else {
                File srcJar = new File(project.getBasedir(),
                                       "target/" + project.getArtifactId() + "@" + project.getVersion() + ".jar");
                if (!srcJar.exists()) return;
                //copy jar
                File destJar = new File(outputFolder, relativePath + ".jar");
                FileUtils.copyFile(srcJar, destJar);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't copy targets: " + e.getMessage());
        }
    }


    protected void copyDependencies(File outputFolder) throws MojoExecutionException {
        //如果能给指定排除特定依赖引入的其他依赖，就可以少copy许多与平台重复的依赖
        super.setOutputDirectory(outputFolder);
        super.setCopyPom(true);
        super.addParentPoms = true;
        Collection<String> scope = new HashSet<String>();
        scope.add("compile");
        scope.add("runtime");
        getProject().setArtifactFilter(new CumulativeScopeArtifactFilter(scope));
        super.doExecute();

    }

    @Override
    protected void copyArtifact(Artifact artifact, boolean removeVersion, boolean prependGroupId,
                                boolean useBaseVersion, boolean removeClassifier) throws MojoExecutionException {
        File destDir = new File(outputDirectory, artifact.getGroupId());
        File destFile = new File(destDir, artifact.getArtifactId() + "@" + artifact.getVersion() + "." +
                                          artifact.getArtifactHandler().getExtension());
        copyFile(artifact.getFile(), destFile);
    }

    @Override
    protected void copyFile(File artifact, File destFile) throws MojoExecutionException {
        if (destFile.exists() && destFile.lastModified() == artifact.lastModified())
            return;
        super.copyFile(artifact, destFile);
    }

    public void copyPoms(File outputDirectory, Set<Artifact> artifacts, boolean removeVersion, boolean removeClassifier)
            throws MojoExecutionException {
        for (Artifact artifact : artifacts) {
            Artifact pomArtifact = getResolvedPomArtifact(artifact);

            // Copy the pom
            if (pomArtifact.getFile() != null && pomArtifact.getFile().exists()) {
                File destDir = new File(outputDirectory, pomArtifact.getGroupId());
                File destFile =
                        new File(destDir, pomArtifact.getArtifactId() + "@" + pomArtifact.getVersion() + ".pom");
                if (!destFile.exists()) {
                    copyFile(pomArtifact.getFile(), destFile);
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void reduceDependencies(File currFolder, File anotherFolder, boolean keepCurrent)
            throws MojoExecutionException {
        if (!currFolder.exists()) return;
        try {
            List<File> jars = FileUtils.getFiles(currFolder, "*/*.jar", null);
            for (File jar : jars) {
                String relativePath = relativePath(jar);
                String duplicate = FilenameUtils.concat(anotherFolder.getAbsolutePath(), relativePath);
                if (FileUtils.fileExists(duplicate)) {
                    FileUtils.forceDelete(keepCurrent ? new File(duplicate) : jar);
                }
            }
            List<File> poms = FileUtils.getFiles(currFolder, "*/*.pom", null);
            for (File pom : poms) {
                String relativePomPath = relativePath(pom);
                String relativeJarPath = relativePomPath.replace(".pom", ".jar");
                String duplicatePom = FilenameUtils.concat(anotherFolder.getAbsolutePath(), relativePomPath);
                String duplicateJar = FilenameUtils.concat(anotherFolder.getAbsolutePath(), relativeJarPath);
                if (FileUtils.fileExists(duplicatePom)) {
                    FileUtils.forceDelete(keepCurrent ? new File(duplicatePom) : pom);
                }
                if (FileUtils.fileExists(duplicateJar)) {
                    FileUtils.forceDelete(pom);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't reduce depended lib/jars|poms: " + e.getMessage());
        }
    }

    protected void cleanEmptyFolders(File... folders) {
        // Clean all empty folders
        for (File folder : folders) {
            deleteEmptyDir(folder);
        }
    }


    protected void reducePoms(File folder) throws MojoExecutionException {
        try {
            List<File> pomFiles = FileUtils.getFiles(folder, "*/*.pom", null);
            for (File pomFile : pomFiles) {
                File jarFile = new File(pomFile.getPath().replace(".pom", ".jar"));
                Dependency dep;
                try {
                    dep = Dependency.parse(pomFile);
                } catch (InvalidComponentNameException e) {
                    throw new MojoExecutionException("Can't parse component from:" + pomFile);
                }
                String pomXmlPath = "META-INF/maven/" + dep.getGroupId() + "/" + dep.getArtifactId();
                if (!jarFile.exists()) continue;
                JarFile jar;
                try {
                    jar = new JarFile(jarFile);
                } catch (IOException e) {
                    throw new MojoExecutionException("Can't create jar file: " + jarFile.getName(), e);
                }
                ZipEntry mavenFolder = jar.getEntry(pomXmlPath);
                jar.close();
                if (mavenFolder != null) {
                    FileUtils.forceDelete(pomFile);
                } else {
                    File tempJarFolder = extractJar(jarFile);
                    File pomFolder = new File(tempJarFolder, pomXmlPath);
                    FileUtils.forceMkdir(pomFolder);

                    List<String> lines = new ArrayList<String>(3);
                    lines.add("groupId=" + dep.getGroupId());
                    lines.add("artifactId=" + dep.getArtifactId());
                    lines.add("version=" + dep.getVersion());
                    File pomProperties = new File(pomFolder, "pom.properties");
                    //noinspection ResultOfMethodCallIgnored
                    pomProperties.createNewFile();
                    FileOutputStream pomPropertiesStream = new FileOutputStream(pomProperties, false);
                    try {
                        IOUtils.writeLines(lines, "\n", pomPropertiesStream);
                    } finally {
                        pomPropertiesStream.close();
                    }
                    File pomXml = new File(pomFolder, "pom.xml");
                    FileUtils.copyFile(pomFile, pomXml);
                    try {
                        createJar(tempJarFolder, jarFile);
                    } finally {
                        FileUtils.forceDelete(tempJarFolder);
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move/delete poms: " + e.getMessage(), e);
        }

    }

    protected void sealPoms(File folder) throws MojoExecutionException {
        File pomsFolder = new File(folder, "poms");
        try {
            List<File> pomFiles = FileUtils.getFiles(folder, "*/*.pom", null);
            for (File pomFile : pomFiles) {
                File tempPomFile = new File(pomsFolder, relativePath(pomFile));
                org.apache.commons.io.FileUtils.moveFile(pomFile, tempPomFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't aggregate poms file into " + pomsFolder.getPath());
        }
        try {
            File pomsJar = new File(folder, "poms.jar");
            createJar(pomsFolder, pomsJar);
        } finally {
            try {
                FileUtils.forceDelete(pomsFolder);
            } catch (IOException e) {
                //skip it
            }
        }

    }

    protected File extractJar(File jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        File tempJarFolder;
        try {
            String jarFileName = FilenameUtils.getBaseName(jarFile.getName());
            tempJarFolder = new File(System.getProperty("java.io.tmpdir"), jarFileName);
            org.apache.commons.io.FileUtils.deleteDirectory(tempJarFolder);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                File file = new File(tempJarFolder, entry.getName());
                if (entry.isDirectory()) {
                    org.apache.commons.io.FileUtils.forceMkdir(file);
                    continue;
                } else {
                    org.apache.commons.io.FileUtils.touch(file);
                }
                FileOutputStream outStream = new FileOutputStream(file);
                try {
                    IOUtils.copy(jarFile.getInputStream(entry), outStream);
                } finally {
                    IOUtils.closeQuietly(outStream);
                }
            }
        } finally {
            jarFile.close();
        }
        return tempJarFolder;
    }

    protected void createJar(File srcFolder, File destJar) {
        if( !srcFolder.exists() ) return;
        Jar jar = new Jar();
        jar.setProject(ANT_PROJECT);
        jar.setUpdate(false);
        jar.setDestFile(destJar);
        File manifestFile = new File(srcFolder, "META-INF/MANIFEST.MF");
        if (manifestFile.exists())
            jar.setManifest(manifestFile);
        FileSet fs = new FileSet();
        fs.setProject(ANT_PROJECT);
        fs.setDir(srcFolder);
        jar.add(fs);
        jar.execute();
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    static void deleteEmptyDir(File dir) {
        if (dir.isDirectory()) {
            File[] fs = dir.listFiles();
            if (fs != null && fs.length > 0) {
                for (File tmpFile : fs) {
                    if (tmpFile.isDirectory()) {
                        deleteEmptyDir(tmpFile);
                    }
                    if (tmpFile.isDirectory() && tmpFile.listFiles().length <= 0) {
                        tmpFile.delete();
                    }
                }
            }
            if (dir.isDirectory() && dir.listFiles().length == 0) {
                dir.delete();
            }
        }
    }
}
