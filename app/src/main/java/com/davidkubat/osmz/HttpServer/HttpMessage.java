package com.davidkubat.osmz.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class HttpMessage {

    private final String source;
    private final MsgType type;
    private final Date createdAt;
    private String desiredObject;
    private String httpVersion;
    private Socket client;
    private ArrayList<String> headers;
    private String content;


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

        BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
        ERROR, GET, PUT, POST, DELETE, OK, UPDATE
    }

}
