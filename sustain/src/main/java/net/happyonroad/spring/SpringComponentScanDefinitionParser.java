/**
 * @author XiongJie, Date: 13-11-4
 */
package net.happyonroad.spring;


import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;

/**
 * <h1>加速Component Scan过程</h1>
 *  加速方式：通过扫描候选资源时，限制在当前application的context里面扫描来实现
 */
public class SpringComponentScanDefinitionParser extends ComponentScanBeanDefinitionParser {
    @Override
    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        return new SpringClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters);
    }


}
