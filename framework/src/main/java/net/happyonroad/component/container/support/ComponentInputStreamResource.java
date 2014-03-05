/**
 * @author XiongJie, Date: 13-11-11
 */
package net.happyonroad.component.container.support;

import net.happyonroad.component.core.Component;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** 基于组件的Spring Resource */
public class ComponentInputStreamResource extends InputStreamResource {
    private final Component component;

    public ComponentInputStreamResource(Component component,
                                        InputStream inputStream,
                                        String description) {
        super(inputStream, description); //description is the path
        this.component = component;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        String thePath = relative(getDescription(), relativePath);
        InputStream relativeStream = component.getResource().getInputStream(thePath);
        return new ComponentInputStreamResource(component, relativeStream, thePath);
    }

    private String relative(String referred, String relativePath) {
        return new File(referred).getParent() + "/" + relativePath;
    }
}
