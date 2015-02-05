/**
 * xiongjie on 14-8-6.
 */
package net.happyonroad.component.container;

import org.springframework.context.ApplicationContext;

/**
 * <h1>Repository Scanner</h1>
 */
public interface RepositoryScanner {
    void bind(ApplicationContext context);
    /**
     * Configure scanner by config resource location
     *
     * @param config the config resource location
     * @throws Exception any exception while configure
     */
    void configure(String config) throws Exception;

    /**
     * Scan mybatis repository/mapper xml in given packages
     *
     * @param packages the packages to be scanned
     * @return scanned packages
     */
    int scan(String... packages);

}
