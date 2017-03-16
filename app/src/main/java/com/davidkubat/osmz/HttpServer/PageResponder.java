package com.davidkubat.osmz.HttpServer;

import android.os.Environment;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;

public class PageResponder implements HttpMessageConsumer {

    private final File extStoragePath;
    private final HTTPServer httpServer;

    public PageResponder(HTTPServer server) {
        String externalStorageState = Environment.getExternalStorageState();
        if (!externalStorageState.equals(Environment.MEDIA_MOUNTED))
            throw new IllegalStateException("External storage is not mounted");

        this.extStoragePath = Environment.getExternalStorageDirectory();
        this.httpServer = server;
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String desiredFile = msg.getDesiredObject();

        if (desiredFile == null || desiredFile.isEmpty() || "/".equals(desiredFile)) {
            desiredFile = "index.html";
        }
        if (desiredFile.contains("?")) {
            desiredFile = desiredFile.substring(0, desiredFile.indexOf('?'));
        }
        File target = new File(extStoragePath, desiredFile);
        if (!target.exists()) {
            return false;
        }

        String extension = target.getAbsolutePath();
        extension = extension.substring(extension.lastIndexOf('.') + 1);
        //HttpMessage retMsg = new HttpMessage(HttpMessage.MsgType.OK ,msg.getSource());

        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        try {
            HttpMessage message = HttpMessage.buildFromResponse(HttpMessage.MsgType.OK, target.length(), type, new FileInputStream(target), msg);
            message.send();
            httpServer.getConnectionHandler().messageArrived(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
