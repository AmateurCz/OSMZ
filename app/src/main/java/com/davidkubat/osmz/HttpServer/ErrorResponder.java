package com.davidkubat.osmz.HttpServer;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by David Kubát on 19.02.2017.
 */

public class ErrorResponder implements HttpMessageConsumer {

    private final SimpleDateFormat dateFormater;

    public  ErrorResponder(){
        dateFormater = new java.text.SimpleDateFormat("EE, dd MMM yyyy kk:mm:ss");
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if(consumed)
            return false;

        try {
            OutputStream stream = msg.getClient().getOutputStream();
            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(stream));
            writer.write("HTTP/1.1 404 Not Found");
            writer.newLine();
            String dateMsg = String.format("Date: %s GTM", dateFormater.format(new Date()));
            writer.write(dateMsg);
            writer.newLine();
            writer.write("Server: TrololoServer - Android");
            writer.newLine();
            String response = String.format("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">" +
                    "<html>" +
                    "<head>" +
                    "   <title>404 Not Found</title>" +
                    "</head>" +
                    "<body>" +
                    "   <center><h1>Not Found</h1></center>" +
                    "   <center><p>The requested URL %s was not found on this server.</p></center>" +
                    "</body>" +
                    "</html>", "Index.html");
            writer.write("Content-Length: ");
            writer.write(response.length());
            writer.newLine();
            writer.write("Content-Type: text/html; charset=iso-8859-1");
            writer.newLine();
            writer.write("Connection: Closed");
            writer.newLine();
            writer.newLine();
            writer.write(response);
            writer.flush();
        } catch (IOException e) {
            Log.e("HTTP Server", "client.getOutputStream() failed.");
            Log.e("HTTP Server", e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
