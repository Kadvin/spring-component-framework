/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

/** 服务未找到异常 */
public class ServiceNotFoundException extends Exception {
    private final SpringServiceImporter importer;

    public ServiceNotFoundException(SpringServiceImporter importer) {
        super("Can't find service " + importer + " in global registry");
        this.importer = importer;
    }

    @SuppressWarnings("UnusedDeclaration")
    public SpringServiceImporter getImporter() {
        return importer;
    }
}
