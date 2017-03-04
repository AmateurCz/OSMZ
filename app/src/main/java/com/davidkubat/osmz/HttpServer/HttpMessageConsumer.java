package com.davidkubat.osmz.HttpServer;

/**
 * Created by David Kubát on 19.02.2017.
 */

public interface HttpMessageConsumer {
    boolean newHttpMessage(HttpMessage msg, boolean consumed);
}
