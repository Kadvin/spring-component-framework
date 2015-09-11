/**
 * @author XiongJie, Date: 13-12-2
 */
package net.happyonroad.builder;

import net.happyonroad.component.container.support.DefaultComponentRepository;
import net.happyonroad.component.core.support.ComponentUtils;
import net.happyonroad.component.core.support.DefaultComponent;
import net.happyonroad.component.core.support.Dependency;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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

import static net.happyonroad.component.core.support.ComponentUtils.relativePath;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * 打包一个基于Spring Component 框架的项目
 */
@SuppressWarnings("unused unchecked")
@Mojo(name = "package", inheritByDefault = false, defaultPhase = LifecyclePhase.PACKAGE)
public class SpringComponentPackaging extends SpringComponentCopyDependencies {
    private static final Pattern          INTERPOLATE_PTN_A = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.MULTILINE);
    private static final Pattern          INTERPOLATE_PTN_B = Pattern.compile("#\\{([^}]+)\\}", Pattern.MULTILINE);
    private static final Charset          UTF8              = Charset.forName("UTF-8");
    private static final SimpleDateFormat sdf               = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Parameter(defaultValue = "1099")
    int        appPort;
    @Parameter(defaultValue = "")
    String     jvmOptions;
    @Parameter
    String     appName;
    @Parameter
    Properties properties;
    @Parameter
    String     propertyFiles;
    @Parameter
    String     logbackFile;
    @Parameter
    String     folders;
    @Parameter
    String     files;
    @Parameter
    String     frontendNodeModules;
    //Debug port
    @Parameter(defaultValue = "0")
    private int     debug;
    //Jmx port
    @Parameter(defaultValue = "0")
    private int     jmx;
    @Parameter
    private String  appPrefix;
    @Parameter(defaultValue = "true")
    private Boolean wrapper;
    @Parameter
    private String  repositoryBase;

    private String appBoot, appTarget;


    private static String lineSeparator = System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n";

    public void doExecute() throws MojoExecutionException {
        getLog().info("Hello, I'm packaging to " + getTargetFolder().getPath());

        if (StringUtils.isNotBlank(repositoryBase)) {
            this.repositoryBase = FilenameUtils.normalize(this.repositoryBase);
        }

        initAppParams();

        prepareFolders();

        prepareFiles();

        File appFolder = new File(getTargetFolder(), "app");
        File libFolder = new File(getTargetFolder(), "lib");
        File repositoryFolder = getRepositoryFolder();

        copyTargets(libFolder);

        copyDependencies(libFolder);

        //清除与jar重复的pom文件
        reducePoms(libFolder);

        //将repository/lib下面的第三方包与lib下面的归并
        // 因为现在是先extending，而后再packing
        reduceDependencies(libFolder, repositoryFolder, true);

        //将 lib/ repository/ 目录下的pom文件归并到一个压缩文件中
        //以免用户修改其中的内容，导致系统不能运行
        sealPoms(libFolder);
        sealPoms(repositoryFolder);

        moveBootstrapJar();

        Map<String, Object> replaces = createAppOptions();

        interpolateScripts(replaces);

        generateScripts(replaces);

        generateWrapper(replaces);

        if (StringUtils.isNotBlank(frontendNodeModules)) {
            //从各个jar包里面抽取前端程序
            generateFrontendResources();
            //根据需要，将前端程序从各个jar包中删除，降低系统运行时的负担
            reduceFrontendResources();
        }

        // Move app jars from lib to app
        moveAppJars(libFolder, appFolder);

        cleanEmptyFolders(appFolder, libFolder, repositoryFolder);

    }

    @Override
    protected void copyFile(File artifact, File destFile) throws MojoExecutionException {
        String relativePath = relativePath(destFile);
        File repositoryFolder = getRepositoryFolder();
        File preventFile = null;
        if (StringUtils.isNotBlank(repositoryBase)) {
            boolean contains = false;
            try {
                contains = FilenameUtils.directoryContains(repositoryBase, artifact.getPath());
            } catch (IOException e) {
                //skip
            }
            if (contains) {
                //这个文件是Repository文件，不放到lib下
                destFile = new File(repositoryFolder, relativePath);
                File libFolder = new File(getTargetFolder(), "lib");
                preventFile = new File(libFolder, relativePath);
            }
        }
        if (preventFile == null)
            preventFile = new File(repositoryFolder, relativePath);

        try {
            copyFile(artifact, destFile, preventFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't move exist " + preventFile.getPath()
                                             + " to " + destFile.getPath(), e);
        }
    }

    /**
     * <h2>把artifact文件copy到目标文件，如果另外一个相应的文件存在，则移动之</h2>
     *
     * @param artifact 原始文件
     * @param dest     目标文件
     * @param prevent  防止的文件
     */
    protected void copyFile(File artifact, File dest, File prevent) throws IOException, MojoExecutionException {
        if (dest.exists()) {
            if (prevent.exists())
                //noinspection ResultOfMethodCallIgnored
                prevent.delete();
        } else {
            if (prevent.exists())
                org.apache.commons.io.FileUtils.moveFile(prevent, dest);
            else
                super.copyFile(artifact, dest);
        }
    }

    private void initAppParams() throws MojoExecutionException {
        if (appName == null || StringUtils.isBlank(appName)) {
            appName = project.getName() == null ? project.getArtifactId() : project.getName();
        }
        appName = interpolate(appName, getProjectProperties(), 'A');
        if (properties == null) {
            properties = new Properties();
        }
        appTarget = String.format("%s/%s@%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
        try {
            System.setProperty("app.home", getTargetFolder().getCanonicalPath());
        } catch (IOException e) {
            System.setProperty("app.home", getTargetFolder().getAbsolutePath());
        }
    }

    private void prepareFolders() throws MojoExecutionException {
        if (!getTargetFolder().exists()) {
            FileUtils.mkdir(getTargetFolder().getAbsolutePath());
        }
        String[] subFolders = {"bin", "boot", "app", "lib", "lib/poms", "config", "repository", "logs"};
        for (String relativePath : subFolders) {
            try {
                prepareFolder(relativePath);
            } catch (IOException e) {
                throw new MojoExecutionException("Can't prepare sub folder " +
                                                 relativePath + " in " + getTargetFolder().getPath());
            }
        }
        if (StringUtils.isNotBlank(folders)) {
            for (String path : StringUtils.split(folders, ",")) {
                try {
                    File folder = new File(path);
                    File dest = new File(getTargetFolder(), folder.getName());
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
                    String relativePath = substringAfter(file.getAbsolutePath(),
                                                         getProject().getBasedir().getAbsolutePath() + "/");
                    File dest = new File(getTargetFolder(), relativePath);
                    FileUtils.copyFile(file, dest);
                } catch (IOException e) {
                    getLog().error("Can't copy user specified folder: " + path + " because of:" + e.getMessage());
                }
            }
        }

    }

    private void moveBootstrapJar() throws MojoExecutionException {
        File libPath = new File(getTargetFolder(), "lib");
        List<File> bootJars;
        try {
            bootJars = FileUtils.getFiles(libPath, "net.happyonroad/spring-component-framework@*.jar", null);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't search " + libPath.getPath() +
                                             " for net.happyonroad/spring-component-framework@*.jar");
        }
        if (bootJars.isEmpty()) {
            throw new MojoExecutionException("You should configure a runtime dependency to " +
                                             "spring-component-framework!");
        }
        if (bootJars.size() > 1) {
            throw new MojoExecutionException("There are more than one net.happyonroad/spring-component-framework");
        }
        File bootJar = bootJars.get(0);
        File destJar = new File(getTargetFolder(), "boot/" + bootJar.getName());
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

    private void moveAppJars(File libFolder, File appFolder) throws MojoExecutionException {
        Collection<File> appJars = getAppJars(libFolder);
        for (File appJar : appJars) {
            // app jar file = target/lib/group/artifact@version.jar
            // new jar file = target/app/group/artifact@version.jar
            String fileName = FilenameUtils.getName(appJar.getAbsolutePath());
            String groupName = FilenameUtils.getName(appJar.getParent());
            File newFile = new File(appFolder, groupName + "/" + fileName);
            try {
                FileUtils.rename(appJar, newFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Can't move " + fileName + " to app folder", e);
            }
        }
    }

    private void interpolateScripts(Map<String, Object> replaces) throws MojoExecutionException {
        try {
            Collection<File> scripts = listFiles(new File(getTargetFolder(), "bin"), new String[]{"sh", "bat"}, true);
            if (!scripts.isEmpty()) {
                for (File script : scripts) {
                    changeFileA(script.getPath(), replaces);
                    chmod(script);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't interpolate script", e);
        }
    }


    private void generateScripts(Map<String, Object> replaces) throws MojoExecutionException {
        try {
            String[] resourceNames = {"start.bat", "start.sh", "stop.bat", "stop.sh", "check.sh"};
            for (String resource : resourceNames) {
                if (!FileUtils.fileExists(new File(getTargetFolder(), "bin/" + resource).getPath())) {
                    copyResource(resource, "bin", replaces);
                    chmod(resource, "bin");
                }
            }
            if (logbackFile != null) {
                if( logbackFile.startsWith("!")) {
                    FileUtils.copyFile(new File(logbackFile.substring(1)),
                                       new File(getTargetFolder(), "config/logback.xml"));
                }else{
                    copyPropertyFile(new File(logbackFile), replaces);
                }
            } else {
                copyResource("logback.xml", "config", replaces);
            }
            if (propertyFiles != null) {
                for (String path : propertyFiles.split(",")) {
                    copyPropertyFile(new File(path), replaces);
                }
            } else {
                File propertiesFile = new File(getTargetFolder(), "config/" + project.getArtifactId() + ".properties");
                FileOutputStream fos = new FileOutputStream(propertiesFile);
                properties.store(fos, appName + " Properties");
                fos.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't generate script", e);
        }

    }

    private Map<String, Object> createAppOptions() {
        List<String> bootJars;
        try {
            bootJars = FileUtils.getFileNames(new File(getTargetFolder(), "boot"),
                                              "spring-component-framework@*.jar",
                                              null, false);
        } catch (IOException e) {
            return new HashMap<String, Object>();
        }
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
        jvmOptions = jvmOptions.replaceAll("\n", "");
        jvmOptions = jvmOptions.replaceAll("\\s+", " ");
        replaces.put("jvm.options", jvmOptions);
        return replaces;
    }

    private void generateWrapper(Map<String, Object> replaces) throws MojoExecutionException {
        if (!wrapper) {
            getLog().warn("Skip generate wrapper");
            return;
        }
        try {
            copyResources("net/happyonroad/wrapper/bin/", "bin");
            copyResources("net/happyonroad/wrapper/boot/", "boot");
            copyResources("net/happyonroad/wrapper/conf/", "config");

            String appHome = (String) replaces.get("app.home");
            String appName = (String) replaces.get("app.name");
            String mainExecutor = appHome + "/bin/" + StringUtils.lowerCase(appName);

            renameFile(appHome + "/bin/main", mainExecutor);
            chmod(new File(mainExecutor));

            String[] jvmOptionArray = jvmOptions.split(" ");
            for (int i = 0; i < jvmOptionArray.length; i++) {
                String jvmOption = jvmOptionArray[i];
                replaces.put("jvm.option." + (i + 6), jvmOption);
            }

            changeFileB(mainExecutor, replaces);

            changeFileA(appHome + "/config/wrapper.conf", replaces);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't copy classpath resources, check the .index file!");
        }

    }

    private void generateFrontendResources() throws MojoExecutionException {
        DefaultComponentRepository repository = new DefaultComponentRepository(getTargetFolder().getAbsolutePath());
        repository.start();

        Properties mappings = new Properties();
        File[] jarArray = getApplicationJars();
        try {
            repository.sortCandidates(jarArray);
        } catch (Exception ex) {
            throw new MojoExecutionException("Can't sort the files by component dependencies", ex);
        }
        for (File componentJar : jarArray) {
            extractFrontendResources(componentJar, mappings);
        }

        if (FileUtils.fileExists(frontendNodeModules)) {
            try {
                FileUtils.copyDirectoryStructure(new File(frontendNodeModules),
                                                 new File(getTargetFolder(), "webapp/frontend/node_modules"));
            } catch (IOException e) {
                getLog().warn("Can't copy node modules", e);
            }
        }
        FileOutputStream mappingStream = null;
        try {
            mappingStream = new FileOutputStream(new File(getTargetFolder(), "webapp/frontend/.mappings"));
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            Iterator<String> names = mappings.stringPropertyNames().iterator();
            while (names.hasNext()) {
                String name = names.next();
                sb.append(String.format("  \"%s\" : \"%s\"", name, mappings.getProperty(name)));
                if (names.hasNext()) sb.append(",\n");
            }
            sb.append("\n}\n");
            IOUtils.write(sb.toString(), mappingStream);
        } catch (Exception ex) {
            throw new MojoExecutionException("Can't generate mapping file");
        } finally {
            IOUtils.closeQuietly(mappingStream);
        }
    }

    private File[] getApplicationJars() {
        Collection<File> libJars = getAppJars(new File(getTargetFolder(), "lib"));
        Collection<File> repositoryJars = getAppJars(new File(getTargetFolder(), "repository"));
        List<File> allJars = new ArrayList<File>();
        allJars.addAll(libJars);
        allJars.addAll(repositoryJars);
        return allJars.toArray(new File[allJars.size()]);
    }

    private Collection<File> getAppJars(File folder) {
        Collection<File> jars = folder.exists() ? listFiles(folder, new String[]{"jar"}, true) : Collections.EMPTY_SET;
        Iterator<File> it = jars.iterator();
        while (it.hasNext()) {
            File file = it.next();
            String relativePath = ComponentUtils.relativePath(file);
            if (!DefaultComponent.isApplication(relativePath)) {
                it.remove();
            }
        }
        return jars;
    }

    private void extractFrontendResources(File componentJar, Properties mappings) throws MojoExecutionException {
        getLog().debug("Extract frontend resources from:" + componentJar);
        try {
            Map<String, Object> replaces = new HashMap<String, Object>();
            replaces.put("project.version", project.getVersion());
            replaces.put("build.timestamp", sdf.format(new Date()));
            Dependency dep = Dependency.parse(componentJar);
            String source = dep.getGroupId() + "." + dep.getArtifactId();
            JarFile jarFile = new JarFile(componentJar);
            if (jarFile.getEntry("frontend") == null) return;
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("frontend")) {
                    File file = new File(getTargetFolder(), "webapp/" + entry.getName());
                    InputStream stream = jarFile.getInputStream(entry);
                    if (entry.isDirectory())
                        FileUtils.mkdir(file.getAbsolutePath());
                    else {
                        if (entry.getName().endsWith("config.js")) {
                            List<String> strings = IOUtils.readLines(stream);
                            String content = StringUtils.join(strings, "\n");
                            content = interpolate(content, replaces, 'A');
                            org.apache.commons.io.FileUtils.write(file, content);
                        } else {
                            org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, file);
                        }
                        mappings.setProperty(entry.getName(), source);
                    }
                }
            }
            jarFile.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Can't extract frontend resources from " + componentJar.getName(), e);
        }
    }

    private void reduceFrontendResources() throws MojoExecutionException {
        File[] jarArray = getApplicationJars();
        for (File jar : jarArray) {
            JarFile jarFile = null;
            File tempJarFolder = null;
            try {
                jarFile = new JarFile(jar);
                String reduceFrontend = jarFile.getManifest().getMainAttributes().getValue("Reduce-Frontend");
                if ("false".equalsIgnoreCase(reduceFrontend)) continue;
                ZipEntry detailEntry = jarFile.getEntry("META-INF/INDEX.DETAIL");
                if (detailEntry == null) continue;

                List<String> lines = IOUtils.readLines(jarFile.getInputStream(detailEntry));
                int reduces = reduceIndexes(lines);
                if (reduces == 0) continue;

                tempJarFolder = extractJar(jar);
                org.apache.commons.io.FileUtils.deleteDirectory(new File(tempJarFolder, "frontend"));
                FileOutputStream outStream = new FileOutputStream(new File(tempJarFolder, detailEntry.getName()));
                IOUtils.writeLines(lines, "\n", outStream);
                outStream.close();
                createJar(tempJarFolder, jar);
            } catch (IOException e) {
                throw new MojoExecutionException("Can't reduce jar file", e);
            } finally {
                IOUtils.closeQuietly(jarFile);
                org.apache.commons.io.FileUtils.deleteQuietly(tempJarFolder);
            }

        }

    }

    private int reduceIndexes(List<String> lines) {
        Iterator<String> it = lines.iterator();
        int count = 0;
        while (it.hasNext()) {
            String line = it.next();
            if (line.startsWith("frontend")) {
                it.remove();
                count++;
            }
        }
        return count;
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
        replaces.put("app.home", getTargetFolder().getAbsolutePath());
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
        String relative = substringAfter(file.getAbsolutePath(),
                                         project.getBasedir().getAbsolutePath() + File.separator);
        FileUtils.fileWrite(new File(getTargetFolder(), relative), interpolated);
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
        FileUtils.fileWrite(new File(getTargetFolder(), path + "/" + resource), interpolated);
    }

    private void copyResources(String resourcePath, String path) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        InputStream indexStream = cl.getResourceAsStream(resourcePath + ".index");
        try {
            List<String> names = IOUtils.readLines(indexStream, UTF8);
            for (String name : names) {
                InputStream stream = cl.getResourceAsStream(resourcePath + name);
                try {
                    FileUtils.copyStreamToFile(new RawInputStreamFacade(stream),
                                               new File(getTargetFolder(), path + "/" + name));
                } finally {
                    stream.close();
                }
            }
        } finally {
            indexStream.close();
        }
    }

    private void chmod(String resource, String path) {
        File script = new File(getTargetFolder(), path + "/" + resource);
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
        File subFolder = new File(getTargetFolder(), relativePath);
        //noinspection StatementWithEmptyBody
        if (subFolder.exists()) {
            // don't clean it, because extension is build before release now
            // FileUtils.cleanDirectory(subFolder);
        } else {
            FileUtils.forceMkdir(subFolder);
        }
    }
}
