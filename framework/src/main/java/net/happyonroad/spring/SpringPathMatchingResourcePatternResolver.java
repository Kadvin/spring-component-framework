/**
 * @author XiongJie, Date: 13-12-27
 */
package net.happyonroad.spring;

import net.happyonroad.component.core.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The extended spring path matching resource pattern resolver
 * <p/>
 * which can resolve resource starts with <b>classpath?:</b>
 */
public class SpringPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {
    public static final String CLASSPATH_THIS_URL_PREFIX = "classpath?:";
    private final ComponentApplicationContext cac;

    public SpringPathMatchingResourcePatternResolver(ComponentApplicationContext context) {
        super(context);
        this.cac = context;
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        if (locationPattern.startsWith(CLASSPATH_THIS_URL_PREFIX)) {
            String pattern = locationPattern.substring(CLASSPATH_THIS_URL_PREFIX.length());
            Component component = cac.getComponent();

            if(getPathMatcher().isPattern(pattern)){
                String rootDirPath = determineRootDir(pattern);
                String subPattern = pattern.substring(rootDirPath.length());
                Resource[] resources = component.getResource().getLocalResourcesUnder(rootDirPath);
                Set<Resource> matches = new HashSet<Resource>();
                for (Resource resource : resources) {
                    rootDirPath = rootDirPath.replaceAll("\\\\", "/");
                    String resourcePath = resource.getURL().getPath().replaceAll("\\\\", "/");
                    int pos = resourcePath.indexOf(rootDirPath) + rootDirPath.length();
                    String relativePath = resourcePath.substring(pos);
                    if(getPathMatcher().match(subPattern, relativePath)){
                        matches.add(resource);
                    }
                }
                return matches.toArray(new Resource[matches.size()]);
            }else{
                return component.getResource().getLocalResourcesUnder(pattern);
            }
        } else {
            return super.getResources(locationPattern);
        }
    }
}
