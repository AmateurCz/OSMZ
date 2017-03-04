package com.davidkubat.osmz.HttpServer;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by David Kubát on 19.02.2017.
 */

public interface ConnectionHandler {
    void handleConnection(Socket clientSocket);
    void errorOccurred(Exception e);
}
