/**
 * @author XiongJie, Date: 13-11-12
 */
package net.happyonroad.component.core;

import net.happyonroad.spring.context.ComponentApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 特性集合 */
public class Features {
    public static final String STATIC_FEATURE      = "library";
    public static final String APPLICATION_FEATURE = "application";

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

    public ClassLoader getLibraryFeature(){
        return getFeature(STATIC_FEATURE);
    }


    public Set<String> featureNames() {
        return map.keySet();
    }
}
