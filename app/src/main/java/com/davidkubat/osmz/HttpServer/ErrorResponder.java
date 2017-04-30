package com.davidkubat.osmz.HttpServer;

import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayInputStream;

/**
 * Created by David Kubát on 19.02.2017.
 */

public class ErrorResponder implements HttpMessageConsumer {

    private final HTTPServer httpServer;

    public ErrorResponder(HTTPServer server) {
        this.httpServer = server;
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String response = String.format("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">" +
                "<html>" +
                "<head>" +
                "   <title>404 Not Found</title>" +
                "</head>" +
                "<body>" +
                "   <center><h1>Not Found</h1></center>" +
                "   <center><p>The requested URL %s was not found on this server.</p></center>" +
                "</body>" +
                "</html>", msg.getDesiredObject());

        byte[] data;
        try {
            data = response.getBytes("UTF-8");
        } catch (Exception e) {
            data = new byte[0];
        }

        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("html");
        try {
            HttpMessage message = HttpMessage.buildFromResponse(HttpMessage.MsgType.OK, msg.getDesiredObject(), data.length, type, new ByteArrayInputStream(data), msg);
            message.send();
            httpServer.getConnectionHandler().messageArrived(message);
        } catch (Exception e) {
            Log.e("SERVER", e.getMessage());
        }
        return true;
    }
}
