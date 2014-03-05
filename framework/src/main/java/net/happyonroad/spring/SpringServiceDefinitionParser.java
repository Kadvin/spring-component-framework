/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 对Spring Service节点的解析 */
public class SpringServiceDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return SpringServicePackage.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        NodeList childNodes = element.getChildNodes();
        ManagedList<BeanDefinition> exporters = new ManagedList<BeanDefinition>();
        ManagedList<BeanDefinition> importers = new ManagedList<BeanDefinition>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("export".equals(child.getLocalName())) {
                BeanDefinition exporter = parseExporter(child);
                exporters.add(exporter);
            } else if ("import".equals(child.getLocalName())) {
                BeanDefinition importer = parseImporter(child);
                importers.add(importer);
            }
        }
        builder.addPropertyValue("importers", importers);
        builder.addPropertyValue("exporters", exporters);
    }

    private BeanDefinition parseExporter(Node element) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SpringServiceExporter.class);
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("role".equals(child.getLocalName())) {
                builder.addPropertyValue("role", child.getTextContent());
            } else if ("hint".equals(child.getLocalName())) {
                builder.addPropertyValue("hint", child.getTextContent());
            } else if ("ref".equals(child.getLocalName())) {
                builder.addPropertyValue("ref", child.getTextContent());
            }
        }
        return builder.getBeanDefinition();
    }

    private BeanDefinition parseImporter(Node element) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SpringServiceImporter.class);
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("role".equals(child.getLocalName())) {
                builder.addPropertyValue("role", child.getTextContent());
            } else if ("hint".equals(child.getLocalName())) {
                builder.addPropertyValue("hint", child.getTextContent());
            } else if ("as".equals(child.getLocalName())) {
                builder.addPropertyValue("as", child.getTextContent());
            }
        }
        return builder.getBeanDefinition();
    }
}
