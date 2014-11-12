/**
 * Developer: Kadvin Date: 14-5-28 下午3:55
 */
package net.happyonroad.component;

import net.happyonroad.component.container.support.DefaultComponentLoader;
import net.happyonroad.component.container.support.DefaultComponentLoaderTest;
import net.happyonroad.component.container.support.DefaultComponentRepository;
import net.happyonroad.component.core.Component;
import net.happyonroad.util.TempFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

/**
 * 基于组件的测试
 */
public class ComponentTestSupport {
    protected static File    tempFolder = TempFile.tempFolder();
    private static   Project project    = new Project();

    protected DefaultComponentLoader     loader;
    protected DefaultComponentRepository repository;
    protected Component                  target;


    @AfterClass
    public static void tearDownTotal() throws Exception {
        try {
            FileUtils.deleteDirectory(tempFolder);
        } catch (IOException e) {
            e.printStackTrace();//TODO resolve it later
        }
    }

    @Before
    public void setUp() throws Exception {
        repository = new DefaultComponentRepository(tempFolder.getPath());
        loader = new DefaultComponentLoader(repository);
        repository.start();
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (target != null) loader.unload(target);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            repository.stop();
        }
    }

    protected static void createPom(String folder, String compName, File root) throws IOException {
        String path = folder + "/pom.xml";
        URL source = DefaultComponentLoaderTest.class.getClassLoader().getResource(path);
        File destination = new File(root, "lib/poms/" + compName + ".pom");
        FileUtils.copyURLToFile(source, destination);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static void copyJar(String name, File tempFolder) throws IOException {
        URL jar = DefaultComponentLoaderTest.class.getClassLoader().getResource("jars/" + name + ".jar");
        File file = new File(tempFolder, "lib/" + name + ".jar");
        FileUtils.copyURLToFile(jar, file);
    }

    protected static void createJar(String folder, String compName, String classPath, File root) throws IOException {
        createJar(folder, compName, classPath, root, null);
    }

    protected static void createJar(String folder, String compName, String classPath, File root, IOFileFilter filter)
            throws IOException {
        URL metaInf = DefaultComponentLoaderTest.class.getClassLoader().getResource(folder);
        URL classes = DefaultComponentLoaderTest.class.getClassLoader().getResource(classPath);
        assert metaInf != null;
        assert classes != null;
        File destination = new File(root, "temp/" + compName);
        FileUtils.copyDirectory(new File(metaInf.getPath()), destination);
        //FileUtils.copyDirectory(new File(classes.getPath()), new File(destination, classPath));
        Collection<File> classesFiles = FileUtils.listFiles(new File(classes.getPath()), new String[]{"class"}, true);
        for (File classesFile : classesFiles) {
            if (filter != null && !filter.accept(classesFile)) continue;
            String relative = StringUtils.substringAfter(classesFile.getAbsolutePath(), classPath + "/");
            FileUtils.copyFile(classesFile, new File(destination, classPath + "/" + relative));
        }
        File file = new File(root, "lib/" + compName + ".jar");
        Jar jar = new Jar();
        jar.setProject(project);
        jar.setDestFile(file);
        jar.setBasedir(destination);
        File manifest = new File(metaInf.getPath(), "META-INF/MANIFEST.MF");
        if( manifest.exists() ) jar.setManifest(manifest);
        jar.execute();
    }

    protected static class Filter implements IOFileFilter {
        private String prefix;

        public Filter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File file) {
            return file.getName().startsWith(prefix);
        }

        @Override
        public boolean accept(File dir, String name) {
            return true;
        }
    }
}
