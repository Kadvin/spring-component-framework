/**
 * @author XiongJie, Date: 13-11-4
 */
package net.happyonroad.spring;

import org.springframework.context.config.ContextNamespaceHandler;

/** 扩展上下文名字空间解析，加速Component Scan过程 */
public class SpringContextNamespaceHandler extends ContextNamespaceHandler {
    @Override
    public void init() {
        super.init();
        registerBeanDefinitionParser("component-scan", new SpringComponentScanDefinitionParser());
    }
}
