/**
 * @author XiongJie, Date: 13-11-12
 */
package net.happyonroad.component.core;

import net.happyonroad.component.classworld.PomClassRealm;
import net.happyonroad.spring.ComponentApplicationContext;
import net.happyonroad.spring.ServiceApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 特性集合 */
public class Features {
    public static final String STATIC_FEATURE      = "library";
    public static final String APPLICATION_FEATURE = "application";
    public static final String SERVICE_FEATURE = "service";

    private Map<String, Object> map;

    public Features() {
        map = new HashMap<String, Object>();
    }

    public Features(String name, Object feature) {
        this();
        setFeature(name, feature);
    }

    public <T> T getFeature(String name){
        //noinspection unchecked
        return (T) map.get(name);
    }

    public boolean hasFeature(String name){
        return getFeature(name) != null;
    }

    public void setFeature(String name, Object feature){
        map.put(name, feature);
    }

    public Object remove(String name) {
        return map.remove(name);

    }

    public ComponentApplicationContext getApplicationFeature(){
        return getFeature(APPLICATION_FEATURE);
    }

    public void setApplicationFeature(ComponentApplicationContext context){
        setFeature(APPLICATION_FEATURE, context);
    }

    public ServiceApplicationContext getServiceFeature(){
        return getFeature(SERVICE_FEATURE);
    }

    public void setServiceFeature(ServiceApplicationContext context){
        setFeature(SERVICE_FEATURE, context);
    }

    public PomClassRealm getLibraryFeature(){
        return getFeature(STATIC_FEATURE);
    }


    public void setServiceFeature(PomClassRealm realm){
        setFeature(STATIC_FEATURE, realm);
    }

    public Set<String> featureNames() {
        return map.keySet();
    }
}
