package com.davidkubat.osmz.HttpServer;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by David Kubát on 19.02.2017.
 */

public class PageResponder implements HttpMessageConsumer {

    private final File extStoragePath;
    private final SimpleDateFormat dateFormater;

    public PageResponder() {
        String externalStorageState = Environment.getExternalStorageState();
        if (!externalStorageState.equals(Environment.MEDIA_MOUNTED))
            throw new IllegalStateException("External storage is not mounted");

        this.extStoragePath = Environment.getExternalStorageDirectory();
        this.dateFormater = new java.text.SimpleDateFormat("EE, dd MMM yyyy kk:mm:ss");
    }

    @Override
    public boolean newHttpMessage(HttpMessage msg, boolean consumed) {
        if (consumed)
            return true;

        String desiredFile = msg.getDesiredObject();
        if (desiredFile == null || desiredFile.isEmpty() || "/".equals(desiredFile)) {
            desiredFile = "index.html";
        }
        File target = new File(extStoragePath, desiredFile);
        if (!target.exists()) {
            return false;
        }

        String extension = target.getAbsolutePath();
        extension = extension.substring(extension.lastIndexOf('.') + 1);
        //HttpMessage retMsg = new HttpMessage(HttpMessage.MsgType.OK ,msg.getSource());
        try {
            OutputStream stream = msg.getClient().getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write("HTTP/1.1 200 OK");
            writer.newLine();
            String dateMsg = String.format("Date: %s GTM", dateFormater.format(new Date()));
            writer.write(dateMsg);
            writer.newLine();
            writer.write("Server: TrololoServer - Android");
            writer.newLine();
            writer.write("Content-Length: ");
            writer.write(Long.toString(target.length()));
            writer.newLine();
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            writer.write("Content-Type: " + type);
            writer.newLine();
            writer.write("Connection: Closed");
            writer.newLine();
            writer.newLine();

            writer.flush();
            byte[] buffer = new byte[30000];
            FileInputStream fileStream = new FileInputStream(target);
            int readLen = 0;
            while ((readLen = fileStream.read(buffer, 0, buffer.length)) > 0) {
                stream.write(buffer, 0, readLen);
                stream.flush();
            }
            stream.flush();
        } catch (IOException e) {
            Log.e("SERVER", e.getMessage());
        }
        return true;
    }
}
