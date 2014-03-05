/**
 * @author XiongJie, Date: 13-11-27
 */
package com.myapp.basis;

import com.myapp.api.CacheService;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide some basic services
 *   which can be deployed and used by others(server).
 */
@org.springframework.stereotype.Component
public class CacheServiceImpl implements CacheService{
    private Map<String, Object> store = new HashMap<String, Object>();

    @Override
    public boolean store(String key, Object value) {
        store.put(key, value);
        return true;
    }

    @Override
    public Object pick(String key) {
        return store.get(key);
    }
}
