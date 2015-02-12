/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.support;

import java.io.File;

/**
 * 组件工具
 */
public final class ComponentUtils
{

    private ComponentUtils(){}

    public static  String relativePath(String filePath) {
        return relativePath(new File(filePath));
    }

    public static  String relativePath(File file) {
        return file.getParentFile().getName() + "/" + file.getName();
    }

}