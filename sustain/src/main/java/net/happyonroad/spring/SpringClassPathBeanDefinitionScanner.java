/**
 * @author XiongJie, Date: 13-12-27
 */
package net.happyonroad.spring;

import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/** The extended scanner*/
public class SpringClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public SpringClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
    }


    @Override
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
        try {
            ResourcePatternResolver resolver = getResourcePatternResolver();
            if (!(resolver instanceof ComponentApplicationContext)){
                return super.findCandidateComponents(basePackage);
            }
            String packageSearchPath = SpringPathMatchingResourcePatternResolver.CLASSPATH_THIS_URL_PREFIX +
                                       resolveBasePackage(basePackage) + "/" + getResourcePattern();
            Resource[] resources = resolver.getResources(packageSearchPath);
            boolean traceEnabled = logger.isTraceEnabled();
            boolean debugEnabled = logger.isDebugEnabled();
            for (Resource resource : resources) {
                if (traceEnabled) {
                    logger.trace("Scanning " + resource);
                }
                if (resource.isReadable()) {
                    try {
                        MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
                        if (isCandidateComponent(metadataReader)) {
                            ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                            sbd.setResource(resource);
                            sbd.setSource(resource);
                            if (isCandidateComponent(sbd)) {
                                if (debugEnabled) {
                                    logger.debug("Identified candidate component class: " + resource);
                                }
                                candidates.add(sbd);
                            }
                            else {
                                if (debugEnabled) {
                                    logger.debug("Ignored because not a concrete top-level class: " + resource);
                                }
                            }
                        }
                        else {
                            if (traceEnabled) {
                                logger.trace("Ignored because not matching any filter: " + resource);
                            }
                        }
                    }
                    catch (Throwable ex) {
                        throw new BeanDefinitionStoreException(
                                "Failed to read candidate component class: " + resource, ex);
                    }
                }
                else {
                    if (traceEnabled) {
                        logger.trace("Ignored because not readable: " + resource);
                    }
                }
            }
        }
        catch (IOException ex) {
            throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
        }
        return candidates;
    }

    // Hacking

    private String getResourcePattern() {
        try{
            return (String)FieldUtils.readField(this, "resourcePattern", true);
        }catch (Exception ex){
            throw new RuntimeException("Can't hacking the ClassPathBeanDefinitionScanner#resourcePattern", ex );
        }
    }

    private ResourcePatternResolver getResourcePatternResolver(){
        try{
            return (ResourcePatternResolver)FieldUtils.readField(this, "resourcePatternResolver", true);
        }catch (Exception ex){
            throw new RuntimeException("Can't hacking the ClassPathBeanDefinitionScanner#resourcePatternResolver", ex );
        }
    }

}
