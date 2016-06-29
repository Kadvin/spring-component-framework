/**
 * @author XiongJie, Date: 13-12-27
 */
package net.happyonroad.spring.support;

import net.happyonroad.component.core.support.ComponentJarResource;
import net.happyonroad.component.core.support.DefaultComponent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

/**
 * The extended spring path matching resource pattern resolver
 * <p/>
 * 将 classpath*:path/of/package/** / *.class 限制在当前组件的jar中搜索
 */
public class ComponentResourcePatternResolver extends PathMatchingResourcePatternResolver {
    static final String CLASSPATH_THIS_URL_PREFIX = "classpath?:";

    final DefaultComponent component;

    public ComponentResourcePatternResolver(DefaultComponent component) {
        super(component.getResource());
        this.component = component;
    }

    @Override
    public Resource getResource(String location) {
        if (location.startsWith(CLASSPATH_THIS_URL_PREFIX)) {
            location = location.substring(CLASSPATH_THIS_URL_PREFIX.length());
            return component.getResource().getLocalResourceUnder(location);
        } else {
            return super.getResource(location);
        }
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        //由于在Annotation模式下，非常难以对ClassPathBeanDefinitionScanner进行配置
        //所以，这里我先采用对 location pattern 进行篡改的方式
        //这个篡改，基于的前提十分脆弱：就是仅有 component-scan 会生成 classpath*/xx/**/*.class: 形式的pattern
        //如果有其他真的需要跨组件搜索的情况，正好其pattern后面又增加了 **/*.class，那么这里将会
        //导致其他的情况无法工作
        //实际测试下来，发现这个修改可以工作，而且还意外的对其他几种对象搜索情况进行了加速
        //  例如 spring-data repository/model 的搜索
        //仔细考虑，其实做这个篡改，与我设计这个组件的隔离思路一致
        // 也就是，如果开发者在组件中说要搜索某个包，那么按照隔离的原则，就应该只限定在本jar包中搜索
        //不能搜索到依赖的包中，否则就破坏原本的隔离初衷
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX) && locationPattern.endsWith("**/*.class")) {
            String pattern = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
//            if( component == null ){
//                component = ((ComponentApplicationContext)getResourceLoader()).getComponent();
//            }
            if (getPathMatcher().isPattern(pattern)) {
                String rootDirPath = determineRootDir(pattern);
                String subPattern = pattern.substring(rootDirPath.length());
                Resource[] resources = component.getResource().getLocalResourcesUnder(rootDirPath);
                Set<Resource> matches = new HashSet<Resource>();
                for (Resource resource : resources) {
                    rootDirPath = rootDirPath.replaceAll("\\\\", "/");
                    String resourcePath = resource.getURL().getPath().replaceAll("\\\\", "/");
                    int pos = resourcePath.indexOf(rootDirPath) + rootDirPath.length();
                    String relativePath = resourcePath.substring(pos);
                    if (getPathMatcher().match(subPattern, relativePath)) {
                        matches.add(resource);
                    }
                }
                return matches.toArray(new Resource[matches.size()]);
            } else {
                return component.getResource().getLocalResourcesUnder(pattern);
            }
        } else {
            return super.getResources(locationPattern);
        }
    }

    @Override
    protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, String subPattern)
            throws IOException {
        if( !(rootDirResource instanceof ClassPathResource)){
            return super.doFindPathMatchingJarResources(rootDirResource, subPattern);
        }

        JarFile jarFile = ((ComponentJarResource) component.getResource()).getJarFile();
        String rootEntryPath;

        JarEntry jarEntry = jarFile.getJarEntry(((ClassPathResource)rootDirResource).getPath());
        rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");

        if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
            // Root entry path must end with slash to allow for proper matching.
            // The Sun JRE does not return a slash here, but BEA JRockit does.
            rootEntryPath = rootEntryPath + "/";
        }
        Set<Resource> result = new LinkedHashSet<Resource>(8);
        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            String entryPath = entry.getName();
            if (entryPath.startsWith(rootEntryPath)) {
                String relativePath = entryPath.substring(rootEntryPath.length());
                if (getPathMatcher().match(subPattern, relativePath)) {
                    result.add(rootDirResource.createRelative(relativePath));
                }
            }
        }
        return result;

    }
}
