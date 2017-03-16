package com.davidkubat.osmz.HttpServer;

import java.net.Socket;

public interface ConnectionHandler {
    void handleConnection(Socket clientSocket);
    void errorOccurred(Exception e);

    void messageArrived(HttpMessage httpMessage);
}
