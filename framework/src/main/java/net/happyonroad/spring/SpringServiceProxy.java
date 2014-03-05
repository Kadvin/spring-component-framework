/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/** Description */
public class SpringServiceProxy {
    protected static Logger logger = LoggerFactory.getLogger(SpringServiceExporter.class);
    protected String             role;
    protected Class              roleClass;
    private   String             hint;
    private   ApplicationContext context;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    protected Class getRoleClass() throws ServiceConfigurationException {
        if (roleClass == null) {
            try {
                roleClass = Class.forName(getRole(), true, context.getClassLoader());
            } catch (Exception ex) {
                throw new ServiceConfigurationException("Can't resolve role class: [" +
                                                        getRole() + "] by class context: " + context, ex);
            }
        }
        return roleClass;
    }


    public void bind(ApplicationContext loader) {
        this.context = loader;
    }
}
