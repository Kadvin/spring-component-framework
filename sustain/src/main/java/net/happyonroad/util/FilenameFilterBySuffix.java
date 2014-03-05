/**
 * @author XiongJie, Date: 13-9-16
 */
package net.happyonroad.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * 按照后缀名进行过滤的文件过滤器
 */
public class FilenameFilterBySuffix implements FilenameFilter{
    private final String suffix;

    public FilenameFilterBySuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
    }
}
