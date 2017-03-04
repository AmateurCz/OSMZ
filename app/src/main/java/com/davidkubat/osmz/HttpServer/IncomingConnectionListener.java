package com.davidkubat.osmz.HttpServer;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;

import javax.net.ServerSocketFactory;

/**
 * Created by David Kubát on 19.02.2017.
 */

public class IncomingConnectionListener{

    private Context context;
    private ConnectionHandler listener;
    private boolean isRunning;
    private int port;
    private ServerSocket socket;

    public boolean isRunning() {
        return isRunning;
    }
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if(isRunning)
            throw  new IllegalStateException("You cant change port when listener is active.");
        this.port = port;
    }

    public void setConnectionHandler(ConnectionHandler listener){
        this.listener = listener;
    }

    public IncomingConnectionListener(Context context){
        this.context = context;
    }

    public void stop(){
        if(socket == null)
            return;
        try {
            socket.close();
        } catch (IOException e) {}
        finally {
            socket = null;
        }
    }

    public void listenForConnections() {
        isRunning = true;
        try {
            socket = ServerSocketFactory.getDefault()
                    .createServerSocket(port);
            Log.i("HTTP Server",String.format("Listening for incoming connections at: %s:%d",
                    socket.getInetAddress().toString(),
                    port));
            while(listener != null) {
                Socket clientSocket = socket.accept();
                if( clientSocket == null)
                    break;

                Log.i("HTTP Server",String.format("Client connected from: %s:%d",
                        clientSocket.getInetAddress().toString(),
                        clientSocket.getPort()));
                listener.handleConnection(clientSocket);
            }
        }
        catch (SocketException e)
        {
            Log.i("HTTP Server","Server Shutdown");
        }
        catch (IOException e){
            Log.i("HTTP Server",String.format("Client encountered error: %s",e.getMessage()));
            listener.errorOccurred(e);
        }
        catch (Exception e){
            Log.i("HTTP Server",String.format("Client encountered error: %s",e.getMessage()));
            listener.errorOccurred(e);
            e.printStackTrace();
        }
        finally {
            Log.i("HTTP Server","Server is no longer listening for incoming connections");
            isRunning = false;
        }
    }
}
