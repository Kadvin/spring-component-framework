/**
 * @author XiongJie, Date: 13-9-22
 */
package net.happyonroad.component.classworld;

import net.happyonroad.component.core.Component;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.springframework.core.io.Resource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * 扩展 plexus 的缺省 class realm的对象
 */
@ManagedResource(description = "组件资源加载器" )
public class PomClassRealm extends ClassRealm
        implements Comparable<PomClassRealm>, SelfNaming {
    private static Map<String, Class> shortcuts = new HashMap<String, Class>();
    static String CLASSPATH_THIS_URL_PREFIX = "classpath?:";

    PomClassWorld         pomWorld;
    private Component             component;
    private SortedSet<ClassRealm> dependedRealms;
    Map<String, LoadUnit> stats = new HashMap<String, LoadUnit>();

    /*暂时不支持 depends parent == depends parent's all modules*/
    /*如果需要这个特性，那么也应该在组件之间建立depends关系的时候直接翻译为对parent的modules的依赖*/
    //private SortedSet<ClassRealm> moduleRealms;

    public PomClassRealm(PomClassWorld world, Component component) {
        super(world, component.getId(), getParentClassLoader(world, component));
        this.pomWorld = world;
        this.component = component;
        URL url = this.component.getFileURL();
        if (url != null) addURL(url);
        //将所有的第三方包的类由一个统一的class loader加载，管理
        //而后这个class loader面向不同的组件，有许多 representation
        //跨过各种依赖关系，直接将第三方包依赖加到本身上，加速系统启动过程中的Class Load过程
        Set<URL> libUrls = this.component.getDependedPlainURLs();
        if(libUrls != null){
            ClassLoaderRepresentation representation = new ClassLoaderRepresentation(libUrls);
            setParentClassLoader(representation);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public Component getComponent() {
        return component;
    }

    @Override
    public URL getResource(String name) {
        if(name.startsWith(CLASSPATH_THIS_URL_PREFIX)){
            name = name.substring(CLASSPATH_THIS_URL_PREFIX.length());
            Resource resource = getComponent().getResource().getLocalResourceUnder(name);
            try {
                return resource == null ? null : resource.getURL();
            } catch (IOException e) {
                return null;
            }
        }
        return super.getResource(name);
    }
// ------------------------------------------------------------
    //     扩展功能，统计类加载情况
    // ------------------------------------------------------------

    @Override
    public Class loadClassFromSelf(String name) {
        try {
            Class klass = shortcuts.get(name);
            if(klass != null) return klass;
            else {
                klass = super.findSystemClass(name);
                loaded(name, "self#system");
                return klass;
            }
        } catch (ClassNotFoundException e) {
            Class klass = super.loadClassFromSelf(name);
            if(klass != null) loaded(name, "self#url");
            return klass;
        }
    }

    public Class loadClassFromDepends(String name) {
        for (ClassRealm dependedRealm : dependedRealms) {
            try {
                Class klass = dependedRealm.loadClass(name);
                loaded(name, "depends#" + dependedRealm.getId());
                return klass;
            } catch (ClassNotFoundException e) {
                /*continue; to try next realm*/
            }
        }
        return null;
    }

    @Override
    public Class loadClassFromParent(String name) {
        Class klass = super.loadClassFromParent(name);
        if(klass != null) loaded(name, "parent");
        return klass;
    }

    public URL loadResourceFromDepends(String name) {
        for (ClassRealm dependedRealm : dependedRealms) {
            URL resource = dependedRealm.getResource(name);
            if( resource != null)
                return resource;
        }
        return null;
    }

    public Enumeration loadResourcesFromDepends(String name) {
        ArrayList<URL> founds = new ArrayList<URL>();
        for (ClassRealm dependedRealm : dependedRealms) {
            Enumeration<URL> resources = null;
            try {
                resources = dependedRealm.getResources(name);
            } catch (IOException e) {
                /*continue;*/
            }
            if(resources != null ){
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    founds.add(url);
                }
            }
        }
        return Collections.enumeration(founds);
    }

    // ------------------------------------------------------------
    //     对 PomClassRealm的扩展
    // ------------------------------------------------------------

    void afterConstruction(){
        customizeStrategy();
        dependRealms();
        //createModuleRealms();
    }

    protected void customizeStrategy() {
        try{
            Field strategyField = getClass().getSuperclass().getDeclaredField("strategy");
            strategyField.setAccessible(true);
            Strategy strategy = new PomStrategy(this);
            strategyField.set(this, strategy);
        }catch (Exception ex){
            throw new UnsupportedOperationException("The parent ClassRealm don't support PomClassRealm hacking by refection", ex);
        }
    }

    protected void dependRealms() {
        dependedRealms = new TreeSet<ClassRealm>();
            for(Component depended : component.getAllDependedComponents()){
                if(depended.isPlain())continue;//所有的第三方包不再提供class realm
                PomClassRealm dependedRealm = findOrCreateRealm(pomWorld, depended);
                dependedRealms.add(dependedRealm);
            }
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") PomClassRealm another) {
        return this.component.compareTo(another.component);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // In order to covert the spring namespace handlers
        if("META-INF/spring.handlers".equals(name)){
            return arrange(super.getResources(name));
        }else
            return super.getResources(name);
    }

    private Enumeration<URL> arrange(Enumeration<URL> resources) {
        ArrayList<URL> commons = new ArrayList<URL>();
        URL special = null;
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if(url.getPath().indexOf("spring-component-framework") > 0 )
                special = url;
            else
                commons.add(url);
        }
        if(special != null) commons.add(special);
        return Collections.enumeration(commons);
    }

    // ------------------------------------------------------------
    //     可管理特性
    // ------------------------------------------------------------

    @ManagedAttribute
    @Override
    public String getId() {
        return super.getId();
    }

    @ManagedAttribute
    public String getURL(){
        URL[] urls = getURLs();
        return urls == null || urls.length == 0 ? "" : urls[0].getPath();
    }

    @Override
    public URL[] getURLs() {
        return super.getURLs();
    }


//    protected void createModuleRealms() {
//        moduleRealms = new TreeSet<ClassRealm>();
//        if(component.getModules() != null && component.getModules().isEmpty()){
//            for(Component module : component.getModules()){
//                PomClassRealm moduleRealm = findOrCreateRealm(pomWorld, module);
//                moduleRealms.add(moduleRealm);
//            }
//        }
//    }

    static ClassLoader getParentClassLoader(PomClassWorld world, Component component) {
        Component parent = component.getParent();
        if (parent == null || parent.isPlain()) {
            //采用一个隔离的Class Loader的根，而不是当前运行环境的根
            return ClassLoader.getSystemClassLoader();
        } else {
            return findOrCreateRealm(world, parent);
        }
    }

    static PomClassRealm findOrCreateRealm(PomClassWorld world, Component component){
        PomClassRealm existRealm = world.getClassRealm(component);
        if(existRealm != null )
            return existRealm;
        try {
            return world.newRealm(component);
        } catch (DuplicateRealmException e) {
            throw new IllegalStateException("I'v checked the realm/component duplication, but it occurs." +
                                                    " this error occurred by concurrent issue!");
        }
    }

    static {
        shortcuts.put("int", int.class);
        shortcuts.put("long", long.class);
        shortcuts.put("short", short.class);
        shortcuts.put("byte", byte.class);
        shortcuts.put("double", double.class);
        shortcuts.put("float", float.class);
        shortcuts.put("boolean", boolean.class);
        shortcuts.put("char", char.class);
        shortcuts.put("void", void.class);
    }

    @Override
    public ObjectName getObjectName() {
        String name = component.getBriefId();
        try {
            return new ObjectName("spring.realms:name=" + name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Can't create object name:" + name);
        }
    }

    // ------------------------------------------------------------
    //     统计支持
    // ------------------------------------------------------------

    LoadUnit loading(String className) {
        LoadUnit unit = stats.get(className);
        if (unit == null) {
            unit = new LoadUnit();
            stats.put(className, unit);
        }
        unit.loading();
        return unit;
    }

    void loaded(String className, String hit) {
        LoadUnit unit = stats.get(className);
        unit.loaded(hit);
    }

    void missing(String className){
        LoadUnit unit = stats.get(className);
        unit.missing();
    }

    @ManagedOperation
    public void dump(String fileName) throws FileNotFoundException {
        PrintStream stream = new PrintStream(fileName);
        try {
            stream.append(getId()).append("\n");
            dump(stream);
            stream.append("\n");
        } finally {
            stream.close();
        }
    }

    @ManagedOperation
    public void reset(){
        stats.clear();
    }

    void dump(PrintStream stream){
        for (Map.Entry<String, LoadUnit> subEntry : stats.entrySet()) {
            //class name
            stream.append("  ").append(subEntry.getKey()).append("\n");
            //stats info
            subEntry.getValue().dump(stream);
        }
        stream.flush();
    }

}
