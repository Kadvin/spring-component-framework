/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring.service;

import net.happyonroad.spring.exception.ServiceConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * The parent class of service importer/exporter
 */
public class SpringServiceProxy {
    protected static Logger logger = LoggerFactory.getLogger(SpringServiceExporter.class);
    protected String             role;
    protected Class[]            roleClasses;
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

    public Class getRoleClass() throws ServiceConfigurationException {
        return getRoleClasses()[0];
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRoleClass(Class roleClass){
        setRoleClasses(new Class[]{roleClass});
    }

    public void setRoleClasses(Class[] roleClasses) {
        this.roleClasses = roleClasses;
        if( roleClasses != null ){
            StringBuilder sb = new StringBuilder();
            for (Class roleClass : roleClasses) {
                sb.append(roleClass.getName()).append(",");
            }
            if( sb.length() > 0 ) sb.deleteCharAt(sb.length()-1);
            this.role = sb.toString();

        }else{
            this.role = null;
        }
    }

    public Class[] getRoleClasses() throws ServiceConfigurationException {
        if (roleClasses == null) {
            String[] roles = StringUtils.split(getRole(), ",");
            roleClasses = new Class[roles.length];
            for (int i = 0; i < roles.length; i++) {
                String role = StringUtils.trim(roles[i]);
                try {
                    roleClasses[i] = Class.forName(role, true, context.getClassLoader());
                } catch (Exception ex) {
                    throw new ServiceConfigurationException("Can't resolve role class: [" +
                                                            role + "] by class context: " + context, ex);
                }
            }
        }
        return roleClasses;
    }


    public void bind(ApplicationContext loader) {
        this.context = loader;
    }
}
