/**
 * @author XiongJie, Date: 13-11-27
 */

package com.myapp.api;

/**
 * The Router API, used by Worker or Caller
 */
public interface RouteAPI {
    /**
     * A service export to client to register itself
     *
     * @param workerId the client identifier
     * @param address  the client address
     * @return client token assigned by the server
     */
    String register(String workerId, String address);

    /**
     * Receive some job assigned by outer system
     * and the router will pick a worker to perform the job, cache the result.
     *
     * @param job the job to be performed
     * @return job result
     */
    Object perform(String job);
}
