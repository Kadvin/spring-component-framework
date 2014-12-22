/**
 * Developer: Kadvin Date: 14/12/19 下午3:37
 */
package net.happyonroad.spring.config;

import net.happyonroad.spring.service.AbstractServiceConfig;

/**
 * 缺省的框架扩展应用服务配置（定义默认导入哪些服务）
 *
 * @see  DefaultAppConfig
 */
public class DefaultServiceConfig extends AbstractServiceConfig {
    @Override
    public void defineServices() {
        // import/export nothing services
    }
}
