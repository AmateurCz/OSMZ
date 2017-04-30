package com.davidkubat.osmz.HttpServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class HttpMessage {

    private static final SimpleDateFormat dateFormater = new java.text.SimpleDateFormat("EE, dd MMM yyyy kk:mm:ss");
    private final String source;
    private final MsgType type;
    private final Date createdAt;
    private String desiredObject;
    private String httpVersion;
    private Socket client;
    private ArrayList<String> headers;
    private String content;
    private InputStream contentStream;


    public HttpMessage(MsgType type){
        this(type, "Unknown");
    }
    public HttpMessage(MsgType type, String source) {
        this.type = type;
        this.createdAt = Calendar.getInstance().getTime();
        this.source = source;
    }
    public HttpMessage(MsgType type, String source, String content) {
        this(type, source);
        this.content = content;
    }

    public HttpMessage(MsgType type, String httpVersion, String source, String desiredObject, ArrayList<String> headers, Socket client) {
        this(type, source);
        this.headers = headers;
        this.client = client;
        this.httpVersion = httpVersion;
        this.desiredObject = desiredObject;
    }

    public  static HttpMessage BuildFromStream(Socket socket) throws IOException {
        String source = String.format("%s:%d",
                socket.getInetAddress().toString(),
                socket.getPort());
        InputStream stream = socket.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String line;
        line = r.readLine();

        String method = null;
        String httpVersion = null;
        String desiredObject = null;
        ArrayList<String> headers = new ArrayList<>();

        while (line != null && !line.isEmpty()){
            if(method == null) {
                String[] header = line.split(" ");
                method = header[0].trim();
                desiredObject = header[1].trim().replace("%20", " ");
                httpVersion = header[2].trim();
            }
            headers.add(line);
            line = r.readLine();
        }
        if (headers.size() == 0)
            return null;

        //stream.close();
        if(method == null)
            return  new HttpMessage(MsgType.ERROR, source, "Method name is missing.");

        method = method.toUpperCase();
        MsgType type;
        if (method.equals("GET"))
        {
            type = MsgType.GET;
        }else{
            return  new HttpMessage(MsgType.ERROR, source, String.format("Method %s is not implemented.", method));
        }
        return new HttpMessage(type, httpVersion, source, desiredObject, headers, socket);
    }

    public static HttpMessage buildFromResponse(MsgType responseType, String desiredObject, long contentLenght, String mimeType, InputStream content, HttpMessage responseTo) {
        ArrayList<String> headers = new ArrayList<>();

        switch (responseType) {
            case OK:
                headers.add("HTTP/1.1 200 OK");
                break;
            case NOTFOUND:
                headers.add("HTTP/1.1 404 Not Found");
                break;
            default:
                throw new IllegalArgumentException("responseType");
        }
        headers.add(String.format("Date: %s GTM", dateFormater.format(new Date())));
        headers.add("Server: TrololoServer - Android");
        headers.add("Content-Length: " + Long.toString(contentLenght));
        headers.add("Content-Type: " + mimeType);
        headers.add("Connection: close");

        HttpMessage message = new HttpMessage(responseType, "localhost");
        message.headers = headers;
        message.client = responseTo.getClient();
        message.contentStream = content;
        message.desiredObject = desiredObject;
        return message;
    }

    public void send() throws IOException {
        OutputStream stream = client.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        for (String header : this.headers) {
            writer.write(header);
            writer.newLine();
        }
        writer.newLine();
        writer.flush();
        if (contentStream != null) {
            final int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int byteCount;

            do {
                byteCount = contentStream.read(buffer, 0, buffer.length);
                stream.write(buffer, 0, byteCount);
            } while (byteCount == bufferSize);
            stream.flush();
            //stream.close();
        }
    }

    public MsgType getType() {
        return type;
    }

    public Date createdAt() {
        return createdAt;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public String getDesiredObject() {
        return desiredObject;
    }

    public ArrayList<String> getHeaders() {
        return headers;
    }

    public Socket getClient() {
        return client;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public boolean isClientRequest() {
        return this.type == MsgType.GET || this.type == MsgType.POST
                || this.type == MsgType.DELETE || this.type == MsgType.PUT
                || this.type == MsgType.UPDATE;
    }


    public enum MsgType {
        ServerStart, ServerStop,
        ERROR, GET, PUT, POST, UPDATE, DELETE, OK, NOTFOUND
    }

}
