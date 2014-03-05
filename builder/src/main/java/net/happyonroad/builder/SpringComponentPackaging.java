/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromDependencies.CopyDependenciesMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.util.*;

/** The spring component packaging */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "package", inheritByDefault = false, defaultPhase = LifecyclePhase.PACKAGE)
public class SpringComponentPackaging extends CopyDependenciesMojo {
    @Parameter
    private File output;
    @Parameter(defaultValue = "1099")
    private int  appPort;
    @Parameter(defaultValue = "")
    private String jvmOptions;
    @Parameter
    private String appName;
    @Parameter
    private Properties properties;
    @Parameter
    private File propertiesFile;
    @Parameter
    private File logbackFile;
    @Parameter
    private File[] folders;
    //Debug port
    @Parameter
    private int debug;
    //Jmx port
    @Parameter
    private int jmx;

    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm packaging to " + output.getPath());

        initAppParams();

        prepareFolders();

        copyTargets();

        copyDependencies();

        moveBootstrapJar();

        generateScripts();
    }

    private void initAppParams() {
        if(appName == null || StringUtils.isBlank(appName)){
            appName = project.getName() == null ? project.getArtifactId() : project.getName();
        }
        if(properties == null){
            properties = new Properties();
        }
    }

    private void prepareFolders() throws MojoExecutionException {
        if (!output.exists()) {
            FileUtils.mkdir(output.getAbsolutePath());
        }
        String[] subFolders = {"bin", "boot", "lib", "lib/poms", "config", "repository"};
        for (String relativePath : subFolders) {
            try {
                prepareFolder(relativePath);
            } catch (IOException e) {
                throw new MojoExecutionException("Can't prepare sub folder " +
                                                 relativePath + " in " + output.getPath());
            }
        }
        if(folders != null){
            for (File folder : folders) {
                try {
                    FileUtils.copyDirectory(folder, new File(output, folder.getName()));
                } catch (IOException e) {
                    getLog().error("Can't copy user specified folder: " + folder + " because of:" + e.getMessage());
                }
            }
        }
    }

    private void copyTargets() throws MojoExecutionException {
        String fileName = project.getGroupId() + "." + project.getArtifactId() + "-" + project.getVersion();
        File targetFile = new File( project.getBasedir(),
                                    "target/" + project.getArtifactId() + "-" + project.getVersion() + ".jar");
        try {
            //copy main pom
            FileUtils.copyFile(project.getFile(), new File(output, "lib/poms/" + fileName + ".pom"));
            //copy jar
            FileUtils.copyFile(targetFile, new File(output, "lib/" + fileName + ".jar"));
        } catch (IOException e) {
            throw new MojoExecutionException("Can't copy targets: " + e.getMessage());
        }
    }

    private void copyDependencies() throws MojoExecutionException {
        File libPath = new File(output, "lib");
        super.setOutputDirectory(libPath);
        super.setCopyPom(true);
        super.setPrependGroupId(true);
        super.addParentPoms = true;
        Collection<String> scope = new HashSet<String>();
        scope.add("test");
        getProject().setArtifactFilter(new CumulativeScopeArtifactFilter(scope));
        super.doExecute();

    }

    private void moveBootstrapJar() throws MojoExecutionException {
        File libPath = new File(output, "lib");
        List<File> bootJars;
        try {
            bootJars = FileUtils.getFiles(libPath,
                                          "net.happyonroad.spring-component-framework-*.jar", null);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't search " + libPath.getPath() +
                                             " for net.happyonroad.spring-component-framework-*.jar");
        }
        if (bootJars.isEmpty()) {
            throw new MojoExecutionException("You should configure a runtime dependency to " +
                                             "net.happyonroad.spring-component-framework!");
        }
        if (bootJars.size() > 1) {
            throw new MojoExecutionException("There are more than one net.happyonroad.spring-component-framework");
        }
        File bootJar = bootJars.get(0);
        File destJar = new File(output, "boot/" + bootJar.getName());
        try {
            FileUtils.rename(bootJar, destJar);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move boot jar:" + e.getMessage());
        }
        String bootPom = bootJar.getPath().replace(".jar", ".pom");
        try {
            FileUtils.forceDelete(bootPom);
        } catch (IOException e) {
            getLog().warn("Can't delete unnecessary boot pom " + bootPom + " :" + e.getMessage());
        }
        try {
            List<File> pomFiles = FileUtils.getFiles(libPath, "*.pom", null);
            for (File pomFile : pomFiles) {
                File newPomFile = new File(libPath, "poms/" + pomFile.getName());
                FileUtils.rename(pomFile, newPomFile);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move poms: " + e.getMessage());
        }
    }

    private void generateScripts() throws MojoExecutionException {
        String target = String.format("%s.%s-%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
        try {
            List<String> bootJars = FileUtils.getFileNames(new File(output, "boot"),
                                                           "net.happyonroad.spring-component-framework-*.jar",
                                                           null, false);
            String bootJarName = "boot/" + bootJars.get(0);
            Map<String, Object> replaces = new HashMap<String, Object>();
            replaces.put("app.id", project.getArtifactId());
            replaces.put("app.name", replaceSpaces(appName));
            replaces.put("app.port", appPort);
            replaces.put("app.boot", bootJarName);
            replaces.put("app.target", target);
            if(jvmOptions == null) jvmOptions = "";
            if(debug > 0 ){
                jvmOptions += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + debug;
            }
            if(jmx > 0 ){
                jvmOptions += " -Dcom.sun.management.jmxremote" +
                              " -Dcom.sun.management.jmxremote.local.only=false" +
                              " -Dcom.sun.management.jmxremote.authenticate=false" +
                              " -Dcom.sun.management.jmxremote.ssl=false" +
                              " -Dcom.sun.management.jmxremote.port=" + jmx;
            }
            replaces.put("jvm.options", jvmOptions);
            String[] resourceNames = {"start.bat", "start.sh", "stop.bat", "stop.sh"};
            for (String resource : resourceNames) {
                copyResource(resource, "bin", replaces);
                chmod(resource, "bin");
            }
            if(logbackFile != null && logbackFile.exists()){
                copyFile(logbackFile, replaces);
            }else{
                copyResource("logback.xml", "config", replaces);
            }
            if(propertiesFile != null && propertiesFile.exists()){
                copyFile(propertiesFile, replaces);
            }else{
                File propertiesFile = new File(output, "config/" + project.getArtifactId() + ".properties");
                FileOutputStream fos = new FileOutputStream(propertiesFile);
                properties.store(fos, appName + " Properties");
                fos.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't generate script");
        }

    }

    private Object replaceSpaces(String appName) {
        return appName.replaceAll("\\s", "_");
    }

    private void copyFile(File file, Map<String, Object> replaces) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        List lines = IOUtils.readLines(fis);
        String content = StringUtils.join(lines, lineSeparator);
        String interpolated = interpolate(content, replaces);
        FileUtils.fileWrite(new File(output, "config/" + file.getName()), interpolated);
    }

    private void copyResource(String resource, String path, Map<String, Object> replaces) throws IOException {
        String content = read(resource);
        String interpolated = interpolate(content, replaces);
        FileUtils.fileWrite(new File(output, path + "/" + resource), interpolated);
    }

    private void chmod(String resource, String path) {
        File script = new File(output, path + "/" + resource);
        if(!SystemUtils.IS_OS_WINDOWS){
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"chmod", "u+x", script.getAbsolutePath()});
                process.waitFor();
                if(process.exitValue() != 0){
                    System.err.println("Can't chmod u+x for " + script.getPath() + ", exit value =" + process.exitValue());
                }
            } catch (IOException e) {
                System.err.println("Can't chmod u+x for " + script.getPath() + ", because of:" + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Can't chmod u+x for " + script.getPath() + ", because of:" + e.getMessage());
            }
        }
    }

    private String read(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        List lines = IOUtils.readLines(stream);
        return StringUtils.join(lines, lineSeparator);
    }

    private String interpolate(String content, Map<String, Object> replaces) {
        String newContent = content;
        for (String key : replaces.keySet()) {
            String compiledKey = key.replace(".", "\\.");
            Object o = replaces.get(key);
            newContent = newContent.replaceAll("\\$\\{" + compiledKey + "\\}", o == null ? "" : o.toString());
        }
        return newContent;
    }

    private void prepareFolder(String relativePath) throws IOException {
        File subFolder = new File(output, relativePath);
        if (subFolder.exists()) {
            FileUtils.cleanDirectory(subFolder);
        } else {
            FileUtils.forceMkdir(subFolder);
        }
    }
}
