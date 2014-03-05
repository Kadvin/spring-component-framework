/**
 * @author XiongJie, Date: 13-10-21
 */
package net.happyonroad.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** 对Service Tag的解析 */
public class SpringServiceNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        //use a customized bean definition parser
        SpringServiceDefinitionParser serviceParser = new SpringServiceDefinitionParser();
        this.registerBeanDefinitionParser("service", serviceParser);
    }
}
