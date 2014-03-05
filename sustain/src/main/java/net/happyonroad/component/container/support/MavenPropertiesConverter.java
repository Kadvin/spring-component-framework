/**
 * @author XiongJie, Date: 13-9-17
 */
package net.happyonroad.component.container.support;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.Properties;

/**
* Maven 格式的属性XML序列化/反序列化工具
*/
class MavenPropertiesConverter implements Converter {
    @Override
    public boolean canConvert(Class type) {
        return Properties.class.isAssignableFrom(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final Properties properties = (Properties) source;
        while (properties.propertyNames().hasMoreElements()) {
            String key = (String) properties.propertyNames().nextElement();
            String value = properties.getProperty(key);
            writer.startNode(key);
            writer.setValue(value);
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Properties properties = new Properties();
        while (reader.hasMoreChildren()){
            reader.moveDown();
            String key = reader.getNodeName();
            String value = reader.getValue();
            properties.setProperty(key, value);
            reader.moveUp();
        }
        return properties;
    }
}
