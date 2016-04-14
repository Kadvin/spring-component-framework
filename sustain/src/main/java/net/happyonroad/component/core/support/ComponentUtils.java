/**
 * @author XiongJie, Date: 13-9-3
 */
package net.happyonroad.component.core.support;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 组件工具
 */
public final class ComponentUtils {

    static Pattern NUMBER = Pattern.compile("(\\d+)");

    private ComponentUtils() {
    }

    public static String relativePath(String filePath) {
        return relativePath(new File(filePath));
    }

    public static String relativePath(File file) {
        return file.getParentFile().getName() + "/" + file.getName();
    }

    /**
     * <h2>比较两个版本</h2>
     * 版本不带classifier，两个版本的数字位长度可能并不一样（将会以0补齐），甚至可能不是数字如 1b
     *
     * @param version1 版本1，类似于 1.2
     * @param version2 版本2，类似于 1.1.3
     * @return 比较结果，version1 == version2，返回0， version1 > version2，返回1， version1 < version2 返回-1
     */
    public static int compareVersion(String version1, String version2) {
        String[] array1 = version1.split("\\.");
        String[] array2 = version2.split("\\.");
        int length = Math.max(array1.length, array2.length);
        if (array1.length < length) {
            array1 = align(array1, length);
        }
        if (array2.length < length) {
            array2 = align(array2, length);
        }
        for (int i = 0; i < length; i++) {
            String s1 = array1[i];
            String s2 = array2[i];
            int v = compareValue(s1, s2);
            if (v != 0) return v;
        }
        return 0;
    }

    //补齐数组
    private static String[] align(String[] origin, int length) {
        String[] array = Arrays.copyOf(origin, length);
        for (int i = origin.length; i < length; i++) {
            array[i] = "0";
        }
        return array;
    }

    //比较两个版本位，可能是单纯的数字，如3，也可能包含了字符串，如3b
    public static int compareValue(String v1, String v2) {
        int i1 = parseInt(v1);
        int i2 = parseInt(v2);
        if (i1 > i2) return 1;
        if (i1 < i2) return -1;
        String s1 = parseString(v1);
        String s2 = parseString(v2);
        int v = s1.compareTo(s2);
        if (v > 0) return 1;
        if (v < 0) return -1;
        return 0;
    }

    //从版本位里面解析数字
    private static int parseInt(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            return Integer.valueOf(NUMBER.matcher(string).group(0));
        }
    }

    //从版本位里面解析字符串(移除所有的数字就是字符串)
    private static String parseString(String string) {
        return string.replaceAll("\\d", "");
    }

}