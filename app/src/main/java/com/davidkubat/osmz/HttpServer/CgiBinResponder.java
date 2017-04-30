package com.davidkubat.osmz.HttpServer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

/**
 * Created by david on 30.4.17.
 */

public class CgiBinResponder implements HttpMessageConsumer {

    private final HTTPServer httpServer;

    public CgiBinResponder(HTTPServer server) {
        this.httpServer = server;
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String desiredCmd = msg.getDesiredObject();
        if (!desiredCmd.toLowerCase().startsWith("/cgi-bin/")) {
            return false;
        }


        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray errArray = new JSONArray();
        try {
            int headerLen = "/cgi-bin/".length();
            if (desiredCmd.length() > headerLen + 1) {
                desiredCmd = desiredCmd.substring(headerLen);
                try {
                    Log.i("CgiBin", "Cmd: " + desiredCmd);

                    ProcessBuilder pb = new ProcessBuilder(desiredCmd.split(" "));
                    pb.redirectErrorStream(false);
                    /* Start the process */
                    Process proc = pb.start();

                    /* Read the process's output */
                    String line;
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            proc.getInputStream()));
                    while ((line = in.readLine()) != null) {
                        array.put(line);
                    }
                    BufferedReader inErr = new BufferedReader(new InputStreamReader(
                            proc.getErrorStream()));
                    while ((line = inErr.readLine()) != null) {
                        errArray.put(line);
                    }

                    /* Clean-up */
                    proc.destroy();
                    System.out.println("Process ended !");

                    object.put("result", "OK");
                    object.put("data", array);
                    object.put("errors", errArray);
                } catch (Exception e) {
                    object.put("result", "ERROR");
                    JSONObject exData = new JSONObject();
                    exData.put("message", e.getMessage());
                    exData.put("localizedMessage", e.getLocalizedMessage());
                    exData.put("stacktrace", e.getStackTrace());
                    object.put("data", exData);
                }
            } else {

                object.put("result", "ERROR");
                JSONObject exData = new JSONObject();
                exData.put("message", "Command is missing");
                object.put("data", exData);
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        byte[] data;
        try {
            String json = object.toString(1);
            data = json.getBytes("UTF-8");
        } catch (Exception e) {
            data = new byte[0];
        }

        String type = "application/json";
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
