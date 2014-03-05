/**
 * @author XiongJie, Date: 13-11-27
 */

package com.myapp.api;

/**
 * The Server API, used by client
 * or outer caller
 */
public interface ServerAPI {
    /**
     * A service export to client to register itself
     *
     * @param clientId the client identifier
     * @param address  the client address
     * @return client token assigned by the server
     */
    String register(String clientId, String address);

    /**
     * Receive some job assigned by outer system
     * and the server will pick a client to perform the job, cache the result.
     *
     * @param job the job to be performed
     * @return job result
     */
    Object perform(String job);
}
