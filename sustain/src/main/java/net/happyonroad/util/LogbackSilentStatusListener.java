/**
 * xiongjie on 14-8-7.
 */
package net.happyonroad.util;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * <h1>Class Usage</h1>
 */
public class LogbackSilentStatusListener implements StatusListener {
    @Override
    public void addStatusEvent(Status status) {
        //yield all status event default
    }
}
