package com.davidkubat.osmz.HttpServer;

import android.content.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class HTTPServer {

    private final Context context;
    private IncomingConnectionListener incomingConnectionListener;
    private Thread listenerThread;
    private ArrayList<HttpMessageConsumer> messageConsumers;
    private String localhost;

    public HTTPServer(Context context){
        this.context = context;
        listenerThread = null;
        messageConsumers = new ArrayList<>();
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
                messageArrived(new HttpMessage(HttpMessage.MsgType.ServerStart, localhost));
                IncomingConnectionListener lis = getIncomingConnectionListener();
                lis.setPort(port);
                lis.listenForConnections();
                messageArrived(new HttpMessage(HttpMessage.MsgType.ServerStop, localhost));
            }
        });
        listenerThread.start();
    }

    public void stop() {
        if(listenerThread == null)
            return;
        listenerThread = null;
        getIncomingConnectionListener().stop();
    }

    private IncomingConnectionListener getIncomingConnectionListener() {
        if(incomingConnectionListener == null){
            incomingConnectionListener = new IncomingConnectionListener(context);
            incomingConnectionListener.setConnectionHandler(new ConnectionHandler() {
                @Override
                public void handleConnection(Socket clientSocket) {

                    try {
                        HttpMessage msg = HttpMessage.BuildFromStream(clientSocket);
                        messageArrived(msg);
                        clientSocket.close();
                    }
                    catch (Exception e){
                        String source = String.format("Client connected from: %s:%d",
                                clientSocket.getInetAddress().toString(),
                                clientSocket.getPort());

                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        printWriter.write(e.getMessage());
                        printWriter.write(System.getProperty("line.separator"));
                        e.printStackTrace(printWriter);
                        messageArrived(new HttpMessage(HttpMessage.MsgType.ERROR,
                                source,
                                writer.toString()));
                        e.printStackTrace();
                    }
                }

                @Override
                public void errorOccurred(Exception e) {

                    Writer writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    printWriter.write(e.getMessage());
                    printWriter.write(System.getProperty("line.separator"));
                    e.printStackTrace(printWriter);
                    messageArrived(new HttpMessage(HttpMessage.MsgType.ERROR, localhost, printWriter.toString()));
                }
            });
        }
        return incomingConnectionListener;
    }

    public boolean isRunning() {
        return incomingConnectionListener != null && incomingConnectionListener.isRunning();
    }


    private void messageArrived(HttpMessage httpMessage) {
        boolean handled = false;
        for (HttpMessageConsumer var: messageConsumers ) {
            handled |= var.newHttpMessage(httpMessage, handled);
        }
    }

    public void addMsgHandler(HttpMessageConsumer httpMessageConsumer) {
        this.messageConsumers.add(httpMessageConsumer);
    }
}
