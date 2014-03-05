/**
 * @author XiongJie, Date: 13-10-23
 */
package net.happyonroad.util;

/** a simple log utils */
public class LogUtils {
    private LogUtils() {
    }

    /**
     * 输出一条Banner消息
     *
     * @param format    Banner消息格式，内嵌 {}作为占位符，语法应该与Slf4j的一样
     * @param alignLeft 左边对其的宽度
     * @param total     总宽度，在保证左对齐宽度的前提下，总的字符串宽度，如果内容超过，则进行截断
     * @param args      日志的参数
     * @return 可以输出的日志
     */
    public static String banner(String format, int alignLeft, int total, Object... args) {
        String message = format;
        for (Object arg : args) {
            message = message.replaceFirst("\\{\\}", arg == null ? "null" : arg.toString());
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < alignLeft - 1; i++)
            builder.append("*");
        builder.append(" ");
        if (total > alignLeft + message.length()) {
            builder.append(message);
            builder.append(" ");
            for (int i = 0; i < total - 1 - alignLeft - message.length(); i++)
                builder.append("*");
        } else {
            builder.append(message.substring(0, total - alignLeft));
        }
        return builder.toString();
    }

    /**
     * 按照左对齐9个*号 + 1个空格，总长度为140进行默认banner格式化
     *
     * @param format Banner消息格式，内嵌 {}作为占位符，语法应该与Slf4j的一样
     * @param args   日志的参数
     * @return 可以输出的日志
     */
    public static String banner(String format, Object... args) {
        return banner(format, 8, 100, args);
    }
}
