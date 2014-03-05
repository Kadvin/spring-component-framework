/**
 * @author XiongJie, Date: 13-10-30
 */
package net.happyonroad.spring;

import net.happyonroad.component.container.MutableServiceRegistry;
import net.happyonroad.component.core.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;

import java.util.List;

/** 某个jar包中service元素定义出来的一堆服务 */
public class SpringServicePackage implements
        Lifecycle,
        ApplicationListener<ApplicationContextEvent>, ApplicationContextAware {
    private Logger logger = LoggerFactory.getLogger(SpringServicePackage.class);
    private String                 componentName;
    /* 为该组件对象生成的服务上下文，本对象就是通过服务声明驻留在该上下文中的对象 */
    private ApplicationContext     serviceContext;
    /* 该组件内部的应用上下文 */
    private ApplicationContext     componentContext;
    /* 全局的服务注册表 */
    private MutableServiceRegistry serviceRegistry;

    /* 服务中声明要导入的服务 */
    private List<SpringServiceImporter> importers;
    /* 服务中声明要导出的服务 */
    private List<SpringServiceExporter> exporters;
    private boolean                     running;

    ////////////////////////////////////////////////////////////////////////////
    //  面向配置的Getter/Setter
    ////////////////////////////////////////////////////////////////////////////

    public List<SpringServiceExporter> getExporters() {
        return exporters;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setExporters(List<SpringServiceExporter> exporters) {
        this.exporters = exporters;
    }

    public List<SpringServiceImporter> getImporters() {
        return importers;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setImporters(List<SpringServiceImporter> importers) {
        this.importers = importers;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Bean自身Lifecycle事件
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void start() {
        importServices();
        running = true;
    }

    @Override
    public void stop() {
        removeImportedServices();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
////////////////////////////////////////////////////////////////////////////
    //  子上下文事件
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 监听子上下文的事件
     *
     * @param event 子上下文事件
     */
    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        if (event.getSource() == this.serviceContext)
            //不关注所在容器的事件，因为自身的事件自然会导致本对象本启动/停止
            return;
        //但是关注子context的事件
        if (event instanceof ContextRefreshedEvent) {
            //先在refresh这步把引用加上，保证start/stop事件发生时 component context有引用
            this.componentContext = (ApplicationContext) event.getSource();
        } else if (event instanceof ContextStartedEvent) {
            //而后在context启动之后这一步把本对象要暴露的服务暴露出去
            exportServices();
        } else if (event instanceof ContextStoppingEvent) {
            //最后在context结束服务之前，把本对象要停止的服务回收回来
            revokeExportedServices();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //  主要生命周期流程实现
    ////////////////////////////////////////////////////////////////////////////

    private void importServices() {
        logger.info("{} import {} services from registry to service context", this, getImporters().size());
        for (SpringServiceImporter importer : getImporters()) {
            try {
                importer.importService(serviceRegistry, serviceContext);
            } catch (ServiceNotFoundException e) {
                throw new IllegalStateException(componentName + " can't import : " + e.getImporter(), e);
            }
        }
    }

    private void removeImportedServices() {
        logger.info("{} remove {} imported services from service context", this, getImporters().size());
        for (SpringServiceImporter importer : getImporters()) {
            importer.removeService(serviceRegistry, serviceContext);
        }
    }

    private void exportServices() {
        logger.info("{} export {} services to service registry", this, getExporters().size());
        for (SpringServiceExporter exporter : getExporters()) {
            exporter.exportService(serviceRegistry, componentContext);
        }
    }

    private void revokeExportedServices() {
        logger.info("{} revoke {} exported services", this, getExporters().size());
        for (SpringServiceExporter exporter : getExporters()) {
            exporter.revokeService(serviceRegistry, componentContext);
        }
    }

    //  辅助

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.serviceContext = applicationContext;
        try {
            this.serviceRegistry = applicationContext.getBean(MutableServiceRegistry.class);
        } catch (NoSuchBeanDefinitionException e) {
            //in shutdown phase, trigger by lazy access
            return;
        }
        Component component = applicationContext.getBean(Component.class);
        this.componentName = component.getDisplayName();
        for (SpringServiceImporter importer : importers) {
            importer.bind(applicationContext);
        }
        for (SpringServiceExporter exporter : exporters) {
            exporter.bind(applicationContext);
        }
    }

    @Override
    public String toString() {
        return "SpringServices[" + this.componentName + "]";
    }
}
