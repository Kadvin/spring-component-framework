/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import net.happyonroad.component.core.exception.InvalidComponentNameException;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.component.core.support.Dependency;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
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

    private static final Pattern          INTERPOLATE_PTN_A = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.MULTILINE);
    private static final Pattern          INTERPOLATE_PTN_B = Pattern.compile("#\\{([^}]+)\\}", Pattern.MULTILINE);
    private static final Charset          UTF8              = Charset.forName("UTF-8");
    private static final SimpleDateFormat sdf               = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
    private String     propertyFiles;
    @Parameter
    private File       logbackFile;
    @Parameter
    private String     folders;
    @Parameter
    private String     files;
    @Parameter
    private String     frontendNodeModules;
    //Debug port
    @Parameter(defaultValue = "0")
    private int        debug;
    //Jmx port
    @Parameter(defaultValue = "0")
    private int        jmx;
    @Parameter
    private String     appPrefix;
    @Parameter(defaultValue = "true")
    private Boolean    wrapper;

    private String appBoot, appTarget;

    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm packaging to " + output.getPath());

        initAppParams();

        prepareFolders();

        prepareFiles();

        copyTargets();

        copyDependencies();

        //清除与jar重复的pom文件
        movePoms();

        //将 lib/poms repository/poms 目录下的pom文件归并到一个压缩文件中
        //以免用户修改其中的内容，导致系统不能运行
        sealPoms(new File(output, "lib"));
        sealPoms(new File(output, "repository"));

        moveBootstrapJar();

        generateScripts();

        generateWrapper();

        generateFrontendResources();
    }

    private void initAppParams() throws MojoExecutionException {
        if (appName == null || StringUtils.isBlank(appName)) {
            appName = project.getName() == null ? project.getArtifactId() : project.getName();
        }
        appName = interpolate(appName, getProjectProperties(), 'A');
        if (properties == null) {
            properties = new Properties();
        }
        appTarget = String.format("%s.%s-%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private void prepareFolders() throws MojoExecutionException {
        if (!output.exists()) {
            FileUtils.mkdir(output.getAbsolutePath());
        }
        String[] subFolders = {"bin", "boot", "lib", "lib/poms", "config", "repository", "logs"};
        for (String relativePath : subFolders) {
            try {
                prepareFolder(relativePath);
            } catch (IOException e) {
                throw new MojoExecutionException("Can't prepare sub folder " +
                                                 relativePath + " in " + output.getPath());
            }
        }
        if (StringUtils.isNotBlank(folders)) {
            for (String path : StringUtils.split(folders, ",")) {
                try {
                    File folder = new File(path);
                    File dest = new File(output, folder.getName());
                    FileUtils.copyDirectoryStructure(folder, dest);
//                    String[] propertyFiles = FileUtils.getFilesFromExtension(dest.getAbsolutePath(), new String[]{"properties"});
//                    Map<String, Object> projectProperties = getProjectProperties();
//                    for (String propertyFile : propertyFiles) {
//                        changeFileA(propertyFile, projectProperties);
//                    }
                } catch (IOException e) {
                    getLog().error("Can't copy user specified folder: " + path + " because of:" + e.getMessage());
                }
            }
        }
    }

    private void prepareFiles() throws MojoExecutionException {
        if (StringUtils.isNotBlank(files)) {
            for (String path : StringUtils.split(files, ",")) {
                try {
                    File file = new File(path);
                    String relativePath = StringUtils
                            .substringAfter(file.getAbsolutePath(), getProject().getBasedir().getAbsolutePath() + "/");
                    File dest = new File(output, relativePath);
                    FileUtils.copyFile(file, dest);
                } catch (IOException e) {
                    getLog().error("Can't copy user specified folder: " + path + " because of:" + e.getMessage());
                }
            }
        }

    }

    private void copyTargets() throws MojoExecutionException {
        String fileName = project.getGroupId() + "." + project.getArtifactId() + "-" + project.getVersion();
        File newTargetFile = new File(project.getBasedir(), "target/" + fileName + ".jar");
        File defaultTargetFile = new File(project.getBasedir(),
                                          "target/" + project.getArtifactId() + "-" + project.getVersion() + ".jar");
        try {
            //copy main pom
            FileUtils.copyFile(project.getFile(), new File(output, "lib/poms/" + fileName + ".pom"));
            //copy jar
            File output = new File(this.output, "lib/" + fileName + ".jar");
            if (newTargetFile.exists()) {
                FileUtils.copyFile(newTargetFile, output);
            } else if (defaultTargetFile.exists()) {
                FileUtils.copyFile(defaultTargetFile, output);
            } else {
                // the project is empty
                getLog().warn(fileName + ".jar is empty");
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

    private void movePoms() throws MojoExecutionException {
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

    private void sealPoms(File parent) {
        File pomsPath = new File(parent, "poms");
        if( !pomsPath.exists() ) return;
        File pomsJar = new File(parent, "poms.jar");
        try {
            Jar ant = new Jar();
            ant.setProject(ANT_PROJECT);
            ant.setUpdate(true);
            ant.setDestFile(pomsJar);
            FileSet fs = new FileSet();
            fs.setProject(ANT_PROJECT);
            fs.setDir(pomsPath);
            ant.add(fs);
            ant.execute();
        } finally {
            try {
                FileUtils.forceDelete(pomsPath);
            } catch (IOException e) {
                //skip it
            }
        }

    }


    private void generateScripts() throws MojoExecutionException {
        try {
            List<String> bootJars = FileUtils.getFileNames(new File(output, "boot"),
                                                           "net.happyonroad.spring-component-framework-*.jar",
                                                           null, false);
            appBoot = "boot/" + bootJars.get(0);
            Map<String, Object> replaces = getProjectProperties();
            if (jvmOptions == null) jvmOptions = "";
            // don't put port(maybe replaced at last)
            // it may affect sed
            if (debug > 0) {
                jvmOptions += " -agentlib:jdwp=transport=dt_socket,server=y,address=" + debug + ",suspend=n";
            }
            if (jmx > 0) {
                jvmOptions += " -Dcom.sun.management.jmxremote" +
                              " -Dcom.sun.management.jmxremote.port=" + jmx +
                              " -Dcom.sun.management.jmxremote.local.only=false" +
                              " -Dcom.sun.management.jmxremote.authenticate=false" +
                              " -Dcom.sun.management.jmxremote.ssl=false";
            }
            if (StringUtils.isNotBlank(appPrefix)) {
                jvmOptions += " -Dapp.prefix=" + appPrefix;
            }
            replaces.put("jvm.options", jvmOptions);
            String[] resourceNames = {"start.bat", "start.sh", "stop.bat", "stop.sh", "check.sh"};
            for (String resource : resourceNames) {
                copyResource(resource, "bin", replaces);
                chmod(resource, "bin");
            }
            if (logbackFile != null && logbackFile.exists()) {
                copyPropertyFile(logbackFile, replaces);
            } else {
                copyResource("logback.xml", "config", replaces);
            }
            if (propertyFiles != null) {
                for (String path : propertyFiles.split(",")) {
                    copyPropertyFile(new File(path), replaces);
                }
            } else {
                File propertiesFile = new File(output, "config/" + project.getArtifactId() + ".properties");
                FileOutputStream fos = new FileOutputStream(propertiesFile);
                properties.store(fos, appName + " Properties");
                fos.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't generate script");
        }

    }

    private void generateWrapper() throws MojoExecutionException {
        if (!wrapper) {
            getLog().warn("Skip generate wrapper");
            return;
        }
        try {
            copyResources("net/happyonroad/wrapper/bin/", "bin");
            copyResources("net/happyonroad/wrapper/boot/", "boot");
            copyResources("net/happyonroad/wrapper/conf/", "config");

            Map<String, Object> replaces = getProjectProperties();
            String appHome = (String) replaces.get("app.home");
            String appName = (String) replaces.get("app.name");
            String mainExecutor = appHome + "/bin/" + StringUtils.lowerCase(appName);

            renameFile(appHome + "/bin/main", mainExecutor);
            chmod(new File(mainExecutor));

            String[] jvmOptionArray = jvmOptions.split(" ");
            for (int i = 0; i < jvmOptionArray.length; i++) {
                String jvmOption = jvmOptionArray[i];
                replaces.put("jvm.option." + (i + 5), jvmOption);
            }

            changeFileB(mainExecutor, replaces);

            changeFileA(appHome + "/config/wrapper.conf", replaces);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't copy classpath resources, check the .index file!");
        }

    }

    private void generateFrontendResources() throws MojoExecutionException {
        Properties mappings = new Properties();
        IOFileFilter appCompFilter = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return DefaultComponent.isApplication(FileUtils.basename(file.getName()));
            }

            @Override
            public boolean accept(File dir,
                                  String name) {
                return DefaultComponent.isApplication(name);
            }
        };
        AndFileFilter filter = new AndFileFilter(new WildcardFileFilter("*.jar"), appCompFilter);
        Collection<File> libJars = org.apache.commons.io.FileUtils.listFiles(new File(output, "lib"), filter, null );
        Collection<File> repositoryJars = org.apache.commons.io.FileUtils.listFiles(new File(output, "repository"),
                                                                                    filter, null);
        // TODO 没有严格按照组件的依赖顺序进行抽取，会有覆盖问题
        for (File componentJar : libJars) {
            extractFrontendResources(componentJar, mappings);
        }
        for (File componentJar : repositoryJars) {
            extractFrontendResources(componentJar, mappings);
        }

        if( StringUtils.isNotBlank(frontendNodeModules) && FileUtils.fileExists(frontendNodeModules)){
            try {
                FileUtils.copyDirectoryStructure(new File(frontendNodeModules),
                                                 new File(output, "webapp/frontend/node_modules"));
            } catch (IOException e) {
                getLog().warn("Can't copy node modules", e);
            }
        }
        FileOutputStream mappingStream = null;
        try {
            mappingStream = new FileOutputStream(new File(output, "webapp/frontend/.mappings"));
            mappings.store(mappingStream, "frontend mappings");
        } catch (Exception ex){
            throw new MojoExecutionException("Can't generate mapping file");
        } finally {
            IOUtils.closeQuietly(mappingStream);
        }
    }

    private void extractFrontendResources(File componentJar, Properties mappings) throws MojoExecutionException {
        getLog().info("Extract frontend resources from:" + componentJar);
        try {
            Map<String, Object> replaces = new HashMap<String, Object>();
            replaces.put("project.version", project.getVersion());
            replaces.put("build.timestamp", sdf.format(new Date()));
            JarFile jarFile = new JarFile(componentJar);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if( entry.getName().startsWith("frontend")){
                    File file = new File(output, "webapp/" + entry.getName());
                    InputStream stream = jarFile.getInputStream(entry);
                    if( entry.isDirectory() )
                        FileUtils.mkdir(file.getAbsolutePath());
                    else{
                        if( entry.getName().endsWith("config.js")){
                            List<String> strings = IOUtils.readLines(stream);
                            String content = StringUtils.join(strings, "\n");
                            content = interpolate(content, replaces, 'A');
                            org.apache.commons.io.FileUtils.write(file, content);
                        }else{
                            org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, file);
                        }
                        mappings.setProperty(entry.getName(), FileUtils.filename(componentJar.getPath()));
                    }
                }
            }
            jarFile.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Can't extract frontend resources from " + componentJar.getName() , e );
        }
    }


    private void renameFile(String from, String to) throws IOException {
        FileUtils.rename(new File(from), new File(to));
    }

    private Map<String, Object> getProjectProperties() {
        Map<String, Object> replaces = new HashMap<String, Object>();
        Properties projectProperties = project.getProperties();
        for (String property : projectProperties.stringPropertyNames()) {
            replaces.put(property, projectProperties.getProperty(property));
        }
        replaces.put("app.home", output.getAbsolutePath());
        replaces.put("app.name", replaceSpaces(appName));
        replaces.put("app.port", appPort);
        replaces.put("app.boot", appBoot);
        replaces.put("app.target", appTarget);
        return replaces;
    }

    private Object replaceSpaces(String appName) {
        return appName.replaceAll("\\s", "_");
    }

    private void copyPropertyFile(File file, Map<String, Object> replaces) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        List<String> lines = IOUtils.readLines(fis, UTF8);
        String interpolated;
        if (file.getName().toLowerCase().endsWith(".properties")) {
            interpolated = interpolate(lines, replaces, 'A');
        } else {
            String content = StringUtils.join(lines, lineSeparator);
            interpolated = interpolate(content, replaces, 'A');
        }
        String relative = StringUtils
                .substringAfter(file.getAbsolutePath(), project.getBasedir().getAbsolutePath() + File.separator);
        FileUtils.fileWrite(new File(output, relative), interpolated);
    }

    private void changeFileA(String path, Map<String, Object> replaces) throws IOException {
        changeFile(path, replaces, 'A');
    }

    private void changeFileB(String path, Map<String, Object> replaces) throws IOException {
        changeFile(path, replaces, 'B');
    }

    private void changeFile(String path, Map<String, Object> replaces, char mode) throws IOException {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        List<String> lines = IOUtils.readLines(fis, UTF8);
        String interpolated;
        if (file.getName().toLowerCase().endsWith(".properties")) {
            interpolated = interpolate(lines, replaces, mode);
        } else {
            String content = StringUtils.join(lines, lineSeparator);
            interpolated = interpolate(content, replaces, mode);
        }
        FileUtils.fileWrite(file, interpolated);
    }

    private void copyResource(String resource, String path, Map<String, Object> replaces) throws IOException {
        String content = read(resource);
        String interpolated = interpolate(content, replaces, 'A');
        FileUtils.fileWrite(new File(output, path + "/" + resource), interpolated);
    }

    private void copyResources(String resourcePath, String path) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        InputStream indexStream = cl.getResourceAsStream(resourcePath + ".index");
        try {
            List<String> names = IOUtils.readLines(indexStream, UTF8);
            for (String name : names) {
                InputStream stream = cl.getResourceAsStream(resourcePath + name);
                try {
                    FileUtils.copyStreamToFile(new RawInputStreamFacade(stream), new File(output, path + "/" + name));
                } finally {
                    stream.close();
                }
            }
        } finally {
            indexStream.close();
        }
    }

    private void chmod(String resource, String path) {
        File script = new File(output, path + "/" + resource);
        chmod(script);
    }

    private void chmod(File script) {
        if (!SystemUtils.IS_OS_WINDOWS) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"chmod", "u+x", script.getAbsolutePath()});
                process.waitFor();
                if (process.exitValue() != 0) {
                    System.err.println(
                            "Can't chmod u+x for " + script.getPath() + ", exit value =" + process.exitValue());
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
        List lines = IOUtils.readLines(stream, UTF8);
        return StringUtils.join(lines, lineSeparator);
    }

    //interpolate xml, bat file content with ${}
    private String interpolate(String content, Map<String, Object> replaces, char mode) {
        Pattern pattern = mode == 'A' ? INTERPOLATE_PTN_A : INTERPOLATE_PTN_B;
        Matcher m = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            Object replace = replaces.get(variable);
            String replacement = replace == null ? "" : replace.toString();
            if (pattern.matcher(replacement).find())
                replacement = interpolate(replacement, replaces, mode);
            try {
                m.appendReplacement(sb, replacement);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    // interpolate properties file like: http.port=the.default.value
    private String interpolate(List<String> lines, Map<String, Object> replaces, char mode) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.trim().startsWith("#") && line.contains("=")) {
                String[] array = line.split("=");
                String key = array[0].trim();
                // string like: neo4j.user= split array length == 1
                String value;
                if (array.length == 2) {
                    value = array[1];
                } else if (array.length == 1) {
                    value = "";
                } else {
                    continue;//don't touch them
                }
                value = interpolate(value, replaces, mode);
                if (replaces.containsKey(key)) {
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
            // don't clean it, because extension is build before release now
            // FileUtils.cleanDirectory(subFolder);
        } else {
            FileUtils.forceMkdir(subFolder);
        }
    }

    private static Project ANT_PROJECT = new Project();
}
