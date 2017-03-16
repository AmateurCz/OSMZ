package com.davidkubat.osmz.HttpServer;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;

public class FileBrowserResponder implements HttpMessageConsumer {
    private final File extStoragePath;
    private final HTTPServer httpServer;

    public FileBrowserResponder(HTTPServer server) {
        this.httpServer = server;
        String externalStorageState = Environment.getExternalStorageState();
        if (!externalStorageState.equals(Environment.MEDIA_MOUNTED))
            throw new IllegalStateException("External storage is not mounted");

        this.extStoragePath = Environment.getExternalStorageDirectory();
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String desiredFile = msg.getDesiredObject();
        if (!desiredFile.toLowerCase().startsWith("/folderapi")) {
            return false;
        }
        if (desiredFile.contains("?")) {
            String arguments = desiredFile.substring(desiredFile.indexOf('?') + 1);
            desiredFile = "";
            for (String argument : arguments.split("&")) {
                if (argument.startsWith("path=")) {
                    if (argument.equals("path="))
                        desiredFile = "";
                    else
                        desiredFile = argument.substring("path=".length() + 1);
                }
            }
        } else {
            desiredFile = "";
        }
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();

        try {
            File target = new File(extStoragePath, desiredFile);

            if (target.exists()) {
                String storageAbsPath = extStoragePath.getCanonicalPath();
                int charsToCut = storageAbsPath.length() + 1;
                object.put("path", target.getCanonicalPath().substring(charsToCut - 1));

                if (target.isDirectory()) {
                    JSONObject fileDesc;

                    if (!target.getCanonicalPath().equals(storageAbsPath)) {
                        fileDesc = new JSONObject();
                        fileDesc.put("name", "..");
                        fileDesc.put("isDirectory", true);
                        array.put(fileDesc);
                    }

                    for (File f : target.listFiles()) {
                        fileDesc = new JSONObject();
                        String absolutePath = f.getCanonicalPath();
                        fileDesc.put("name", absolutePath.substring(charsToCut));
                        fileDesc.put("isDirectory", f.isDirectory());
                        array.put(fileDesc);
                    }
                    object.put("error", "");
                }
            } else
                object.put("error", desiredFile + " is not valid folder.");


            object.put("files", array);
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

        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("json");
        try {
            HttpMessage message = HttpMessage.buildFromResponse(HttpMessage.MsgType.OK, data.length, type, new ByteArrayInputStream(data), msg);
            message.send();
            httpServer.getConnectionHandler().messageArrived(message);
        } catch (Exception e) {
            Log.e("SERVER", e.getMessage());
        }
        return true;
    }
}
