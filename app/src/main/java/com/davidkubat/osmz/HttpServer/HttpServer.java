package com.davidkubat.osmz.HttpServer;

import android.content.Context;

import java.net.InetAddress;
import java.util.ArrayList;

public class HttpServer {

    private final Context context;
    private HttpServer instance;
    private IncomingConnectionListener incomingConnectionListener;
    private Thread listenerThread;
    private ThreadPoolConnectionHandler threadPoolConnectionHandler;
    private ArrayList<HttpMessageConsumer> messageConsumers;
    private String localhost;

    public HttpServer(Context context) {
        this.context = context;
        listenerThread = null;
        messageConsumers = new ArrayList<>();
        instance = this;
    }

    public void start(){
        this.start(8080);
    }

    public void start(final int port){
        if(listenerThread != null)
            this.stop();

        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    localhost = String.format("%s:%d",
                            InetAddress.getLocalHost().toString(),
                            port);
                } catch (Exception e) {
                    return;
                }

                ConnectionHandler handler = getConnectionHandler();
                handler.messageArrived(new HttpMessage(HttpMessage.MsgType.ServerStart, localhost));
                IncomingConnectionListener lis = getIncomingConnectionListener();
                lis.setPort(port);
                lis.listenForConnections();
                handler.messageArrived(new HttpMessage(HttpMessage.MsgType.ServerStop, localhost));
            }
        });
        listenerThread.start();
    }

    public void stop() {
        if(listenerThread == null)
            return;
        listenerThread = null;
        if (threadPoolConnectionHandler != null)
            threadPoolConnectionHandler.stop();
        getIncomingConnectionListener().stop();
    }

    private IncomingConnectionListener getIncomingConnectionListener() {
        if(incomingConnectionListener == null){
            incomingConnectionListener = new IncomingConnectionListener(context);
            incomingConnectionListener.setConnectionHandler(getConnectionHandler());
        }
        return incomingConnectionListener;
    }

    public boolean isRunning() {
        return incomingConnectionListener != null && incomingConnectionListener.isRunning();
    }

    public void addMsgHandler(HttpMessageConsumer httpMessageConsumer) {
        this.messageConsumers.add(httpMessageConsumer);
    }

    public ConnectionHandler getConnectionHandler() {
        if (threadPoolConnectionHandler == null)
            threadPoolConnectionHandler = new ThreadPoolConnectionHandler(messageConsumers, localhost);
        return threadPoolConnectionHandler;
    }
}
