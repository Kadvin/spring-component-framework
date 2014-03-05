/**
 * @author XiongJie, Date: 13-10-29
 */
package spring.test.api;

/** 服务提供者 */
public interface ServiceProvider {

    /**
     * 一个测试的接口方法
     * @param userName 输入参数
     * @return 输出参数
     */
    String provide(String userName);
}
