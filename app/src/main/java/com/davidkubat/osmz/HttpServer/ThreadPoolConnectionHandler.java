package com.davidkubat.osmz.HttpServer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolConnectionHandler implements ConnectionHandler {

    private final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private final int KEEP_ALIVE_TIME = 500;
    private final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final ArrayList<HttpMessageConsumer> messageConsumers;
    private final String localhost;
    private final ThreadPoolExecutor mThreadPool;
    private final LinkedBlockingQueue<Runnable> mWorkQueue;

    public ThreadPoolConnectionHandler(ArrayList<HttpMessageConsumer> messageConsumers, String localhost) {
        this.messageConsumers = messageConsumers;
        this.localhost = localhost;
        mWorkQueue = new LinkedBlockingQueue<>();
        mThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,       // Initial pool size
                NUMBER_OF_CORES * 16,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mWorkQueue);

    }

    @Override
    public void handleConnection(final Socket clientSocket) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpMessage msg = HttpMessage.BuildFromStream(clientSocket);
                    messageArrived(msg);
                    clientSocket.close();
                } catch (Exception e) {
                    String source = String.format(
                            Locale.getDefault(),
                            "Client connected from: %s:%d",
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
        });
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


    public void messageArrived(HttpMessage httpMessage) {
        boolean handled = false;
        for (HttpMessageConsumer var : messageConsumers) {
            handled |= var.newHttpMessage(httpMessage, handled);
        }
    }

    public void stop() {
        mThreadPool.shutdownNow();
    }
}
