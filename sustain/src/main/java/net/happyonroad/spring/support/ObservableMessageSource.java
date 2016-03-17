package net.happyonroad.spring.support;

import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * <h1>ObservableMessageSource</h1>
 *
 * @author Jay Xiong
 */
public class ObservableMessageSource extends ResourceBundleMessageSource {
    private String[] baseNames;

    @Override
    public ClassLoader getBundleClassLoader() {
        return super.getBundleClassLoader();
    }

    @Override
    public void setBasenames(String... basenames) {
        super.setBasenames(basenames);
        this.baseNames = basenames;
    }

    public String[] getBasenames(){
        return this.baseNames;
    }
}
