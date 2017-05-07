package com.davidkubat.osmz.HttpServer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

/**
 * Created by david on 30.4.17.
 */

public class CameraResponder implements HttpMessageConsumer {

    private final HttpServer httpServer;

    public CameraResponder(HttpServer server) {
        this.httpServer = server;
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String desiredCmd = msg.getDesiredObject();
        if (!desiredCmd.toLowerCase().startsWith("/camera")) {
            return false;
        }


        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        JSONArray errArray = new JSONArray();
        try {
            int headerLen = "/camera".length();
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
