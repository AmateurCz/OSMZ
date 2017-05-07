package com.davidkubat.osmz;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.davidkubat.osmz.HttpServer.CgiBinResponder;
import com.davidkubat.osmz.HttpServer.ErrorResponder;
import com.davidkubat.osmz.HttpServer.FileBrowserResponder;
import com.davidkubat.osmz.HttpServer.HttpMessage;
import com.davidkubat.osmz.HttpServer.HttpMessageConsumer;
import com.davidkubat.osmz.HttpServer.HttpServer;
import com.davidkubat.osmz.HttpServer.PageResponder;

import java.util.concurrent.ArrayBlockingQueue;

public class HttpServerService extends Service {

    public static final String TAG = "HttpServerService";
    public static final String HTTP_MESSAGE = "Message";

    public static ArrayBlockingQueue<HttpMessage> messageQueue;

    private HttpServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServer() {
        if (server != null)
            return;

        if (messageQueue == null)
            messageQueue = new ArrayBlockingQueue<>(32);

        if (server == null) {
            server = new HttpServer(this);
            server.addMsgHandler(new HttpMessageConsumer() {

                @Override
                public boolean newHttpMessage(final HttpMessage msg, boolean consumed) {
                    try {
                        messageQueue.put(msg);
                    } catch (InterruptedException e) {
                    }
                    Intent in = new Intent(HTTP_MESSAGE);
                    sendBroadcast(in);

                    return !msg.isClientRequest();
                }
            });

            server.addMsgHandler(new CgiBinResponder(server));
            server.addMsgHandler(new FileBrowserResponder(server));
            server.addMsgHandler(new PageResponder(server));
            server.addMsgHandler(new ErrorResponder(server));
        }
        int serverPort = 8080;
        server.start(serverPort);
    }

    private void stopServer() {
        server.stop();
        server = null;
    }
}
