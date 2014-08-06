/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.Dependency;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
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

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * 打包一个基于Spring Component 框架的项目
 */
@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "package", inheritByDefault = false, defaultPhase = LifecyclePhase.PACKAGE)
public class SpringComponentPackaging extends CopyDependenciesMojo {

    private static final Pattern INTERPOLATE_PTN = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.MULTILINE);

    @Parameter
    private File       output;
    @Parameter(defaultValue = "1099")
    private int        appPort;
    @Parameter(defaultValue = "")
    private String     jvmOptions;
    @Parameter
    private String     appName;
    @Parameter
    private Properties properties;
    @Parameter
    private File       propertiesFile;
    @Parameter
    private File       logbackFile;
    @Parameter
    private String     folders;
    //Debug port
    @Parameter
    private int        debug;
    //Jmx port
    @Parameter
    private int        jmx;
    @Parameter
    private String     appPrefix;

    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm packaging to " + output.getPath());

        initAppParams();

        prepareFolders();

        copyTargets();

        copyDependencies();

        //清除与jar重复的pom文件
        movePoms();

        moveBootstrapJar();

        generateScripts();
    }

    private void initAppParams() {
        if (appName == null || StringUtils.isBlank(appName)) {
            appName = project.getName() == null ? project.getArtifactId() : project.getName();
        }
        if (properties == null) {
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
        if(StringUtils.isNotBlank(folders)){
            for (String path : StringUtils.split(folders,",")) {
                try {
                    File folder = new File(path);
                    File dest = new File(output, folder.getName());
                    FileUtils.copyDirectoryStructure(folder, dest);
                    String[] propertyFiles = FileUtils.getFilesFromExtension(dest.getAbsolutePath(), new String[]{"properties"});
                    Map<String, Object> projectProperties = getProjectProperties();
                    for (String propertyFile : propertyFiles) {
                        changeFile(propertyFile, projectProperties);
                    }
                } catch (IOException e) {
                    getLog().error("Can't copy user specified folder: " + path + " because of:" + e.getMessage());
                }
            }
        }
    }

    private void copyTargets() throws MojoExecutionException {
        String fileName = project.getGroupId() + "." + project.getArtifactId() + "-" + project.getVersion();
        File newTargetFile = new File(project.getBasedir(), "target/" + fileName + ".jar");
        File defaultTargetFile = new File( project.getBasedir(),
                                    "target/" + project.getArtifactId() + "-" + project.getVersion() + ".jar");
        try {
            //copy main pom
            FileUtils.copyFile(project.getFile(), new File(output, "lib/poms/" + fileName + ".pom"));
            //copy jar
            File output = new File(this.output, "lib/" + fileName + ".jar");
            if(newTargetFile.exists()){
                FileUtils.copyFile(newTargetFile, output);
            }else{
                FileUtils.copyFile(defaultTargetFile, output);
            }
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
            bootJars = FileUtils.getFiles(libPath, "net.happyonroad.spring-component-framework-*.jar", null);
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
    }

    private void movePoms() throws MojoExecutionException{
        File libPath = new File(output, "lib");
        File tempFolder = new File(libPath, "temp");
        try {
            List<File> pomFiles = FileUtils.getFiles(libPath, "*.pom", null);
            for (File pomFile : pomFiles) {
                File newPomFile = new File(libPath, "poms/" + pomFile.getName());
                File jarFile = new File(pomFile.getPath().replace(".pom", ".jar"));
                Dependency dep;
                try {
                    dep = Dependency.parse(pomFile.getName());
                } catch (InvalidComponentNameException e) {
                    throw new MojoExecutionException("Can't parse component from:" + pomFile);
                }
                String pomXmlPath = "META-INF/maven/" + dep.getGroupId() + "/" + dep.getArtifactId();
                if( jarFile.exists() ){
                    JarFile jar = new JarFile(jarFile);
                    ZipEntry mavenFolder = jar.getEntry(pomXmlPath);
                    jar.close();
                    if( mavenFolder != null ){
                        FileUtils.forceDelete(pomFile);
                    }else{
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
                        if( !created ) throw new MojoExecutionException("Can't create " + pomProperties.getPath());
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
                }
                else{
                    FileUtils.rename(pomFile, newPomFile);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move/delete poms: " + e.getMessage());
        }

    }

    private void generateScripts() throws MojoExecutionException {
        String target = String.format("%s.%s-%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
        try {
            List<String> bootJars = FileUtils.getFileNames(new File(output, "boot"),
                                                           "net.happyonroad.spring-component-framework-*.jar",
                                                           null, false);
            String bootJarName = "boot/" + bootJars.get(0);
            Map<String, Object> replaces = getProjectProperties();
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
            if(StringUtils.isNotBlank(appPrefix)){
                jvmOptions += " -Dapp.prefix=" + appPrefix;
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

    private Map<String, Object> getProjectProperties() {
        Map<String, Object> replaces = new HashMap<String, Object>();
        Properties projectProperties = project.getProperties();
        for (String property : projectProperties.stringPropertyNames()) {
            replaces.put(property, projectProperties.getProperty(property));
        }
        replaces.put("app.name", replaceSpaces(appName));
        replaces.put("app.port", appPort);
        return replaces;
    }

    private Object replaceSpaces(String appName) {
        return appName.replaceAll("\\s", "_");
    }

    private void copyFile(File file, Map<String, Object> replaces) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        List<String> lines = IOUtils.readLines(fis);
        String interpolated;
        if( file.getName().toLowerCase().endsWith(".properties")){
            interpolated = interpolate(lines, replaces);
        }else{
            String content = StringUtils.join(lines, lineSeparator);
            interpolated = interpolate(content, replaces);
        }
        FileUtils.fileWrite(new File(output, "config/" + file.getName()), interpolated);
    }

    private void changeFile(String path, Map<String, Object> replaces)throws IOException{
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        List<String> lines = IOUtils.readLines(fis);
        String interpolated;
        if( file.getName().toLowerCase().endsWith(".properties")){
            interpolated = interpolate(lines, replaces);
        }else{
            String content = StringUtils.join(lines, lineSeparator);
            interpolated = interpolate(content, replaces);
        }
        FileUtils.fileWrite(file, interpolated);
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

    //interpolate xml, bat file content with ${}
    private String interpolate(String content, Map<String, Object> replaces) {
        Matcher m = INTERPOLATE_PTN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            Object replace = replaces.get(variable);
            String replacement = replace == null ? "" : replace.toString();
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    // interpolate properties file like: http.port=the.default.value
    private String interpolate(List<String> lines, Map<String, Object> replaces) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if(!line.trim().startsWith("#") && line.contains("=")){
                String[] array = line.split("=");
                String key = array[0].trim();
                // string like: neo4j.user= split array length == 1
                String value = array.length == 2 ? array[1] : "";
                value = interpolate(value, replaces);
                if( replaces.containsKey(key)){
                    value = replaces.get(key).toString();
                }
                lines.set(i, key + "=" + value);
            }
        }
        return StringUtils.join(lines, lineSeparator);
    }



    private void prepareFolder(String relativePath) throws IOException {
        File subFolder = new File(output, relativePath);
        if (subFolder.exists()) {
            FileUtils.cleanDirectory(subFolder);
        } else {
            FileUtils.forceMkdir(subFolder);
        }
    }

    private static Project ANT_PROJECT = new Project();
}
