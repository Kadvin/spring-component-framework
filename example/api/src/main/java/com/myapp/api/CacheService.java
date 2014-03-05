/**
 * @author XiongJie, Date: 13-11-27
 */

package com.myapp.api;

/**
 * A shared service
 *   which will be used by server
 */
public interface CacheService {
    boolean store(String key, Object value);

    Object pick(String key);
}
