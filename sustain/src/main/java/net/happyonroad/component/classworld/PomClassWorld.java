/**
 * @author XiongJie, Date: 13-8-29
 */
package net.happyonroad.component.classworld;

import net.happyonroad.component.core.Component;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.ClassWorldListener;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 根据Pom创建一个Classworld，其中的Realm为其他依赖的Jar的Pom
 */
@ManagedResource(description = "Pom类体系")
public class PomClassWorld extends ClassWorld implements SelfNaming{
    private Logger logger = LoggerFactory.getLogger(PomClassWorld.class.getName());
    Map<String, PomClassRealm> stolenRealms;
    List<ClassWorldListener>   stolenListeners;
    String                     mainComponentId;

    // -- 统计信息 --
    // realm
    //   |- class A:
    //       |- loading: xx times
    //       |- loaded#self#system: xx times
    //       |- loaded#self#url:    xx times
    //       |- loaded#depended#id: xx times

    public PomClassWorld() {
        try {
            Field realmField = getClass().getSuperclass().getDeclaredField("realms");
            realmField.setAccessible(true);
            //noinspection unchecked
            stolenRealms = (Map<String, PomClassRealm>) realmField.get(this);
            Field listenersField = getClass().getSuperclass().getDeclaredField("listeners");
            listenersField.setAccessible(true);
            //noinspection unchecked
            stolenListeners = (List<ClassWorldListener>) listenersField.get(this);
        } catch (Exception ex) {
            throw new UnsupportedOperationException(
                    "The parent ClassWorld do not support PomClassWorld hacking by refection", ex);
        }
    }

    public PomClassRealm newRealm(Component component) throws DuplicateRealmException {
        String id = component.getId();
        if (getClassRealm(id) != null) {
            throw new DuplicateRealmException(this, id);
        }

        logger.debug("Creating realm for {}", component);
        PomClassRealm realm = new PomClassRealm(this, component);
        stolenRealms.put(id, realm);

        realm.afterConstruction();

        for (ClassWorldListener listener : stolenListeners) {
            listener.realmCreated(realm);
        }
        logger.debug("Created  realm for {}", component);
        return realm;
    }

    @Override
    public synchronized PomClassRealm getClassRealm(String id) {
        return (PomClassRealm) super.getClassRealm(id);
    }

    public synchronized PomClassRealm getClassRealm(Component component) {
        return (PomClassRealm) super.getClassRealm(component.getId());
    }

    @SuppressWarnings("UnusedDeclaration")
    public PomClassRealm getRealm(Component component) throws NoSuchRealmException {
        return (PomClassRealm) getRealm(component.getId());
    }

    public void setMainComponentId(String mainComponentId) {
        this.mainComponentId = mainComponentId;
    }

    public PomClassRealm getMainRealm() {
        return getClassRealm(this.mainComponentId);
    }

    ////////////////////////////////////////////////////////////
    // 统计相关的服务
    ////////////////////////////////////////////////////////////


    @ManagedOperation
    public void dump(String fileName) throws FileNotFoundException {
        PrintStream stream = new PrintStream(fileName);
        try {
            for (Map.Entry<String, PomClassRealm> entry : stolenRealms.entrySet()) {
                stream.append(entry.getKey()).append("\n");
                entry.getValue().dump(stream);
                stream.append("\n");
            }
        } finally {
            stream.close();
        }

    }

    @ManagedOperation
    public void reset(){
        for (PomClassRealm realm : stolenRealms.values()) {
            realm.reset();
        }
    }

    @ManagedAttribute
    public int getRealmCount(){
        return stolenRealms.size();
    }

    @ManagedAttribute
    public int getListenerCount(){
        return stolenListeners.size();
    }

    @ManagedAttribute
    public String getMainComponentId(){
        return mainComponentId;
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return new ObjectName("spring.components:name=world");
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Can't create object name: spring.components:name=world");
        }
    }
}
