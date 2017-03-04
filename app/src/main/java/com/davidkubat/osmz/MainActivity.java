package com.davidkubat.osmz;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.davidkubat.osmz.HttpServer.HTTPServer;
import com.davidkubat.osmz.HttpServer.HttpMessage;
import com.davidkubat.osmz.HttpServer.HttpMessageConsumer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    MessageAdapter adapter;
    ListView messageList;
    Button startButton;
    Button stopButton;

    HTTPServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        messageList = (ListView) findViewById(R.id.message_list);
        messageList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                HttpMessage msg = adapter.getItem(position);
// 2. Chain together various setter methods to set the dialog characteristics

                String detailText = "";
                if(msg.getType() == HttpMessage.MsgType.ERROR){
                    detailText = msg.getContent();
                }else if(msg.isClientRequest()) {
                    StringBuilder sb = new StringBuilder();
                    boolean newLine = false;
                    for (String header : msg.getHeaders()) {
                        if (newLine) {
                            sb.append(System.getProperty("line.separator"));
                        } else
                            newLine = true;
                        sb.append(header.trim());
                    }
                    detailText = sb.toString();
                }
                builder.setMessage(detailText)
                       .setTitle(String.format("%s %s", msg.getType().toString(), msg.getSource()))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

// 3. Get the AlertDialog from create()
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.startServer();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.stopServer();
            }
        });

        adapter = new MessageAdapter(this, new ArrayList<HttpMessage>());
        messageList.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        stopServer();
        super.onPause();
    }

    private void startServer() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        if(server != null && server.isRunning())
            throw new IllegalStateException("Server is already running");
        if(server == null){
            server = new HTTPServer(this);
            server.addMsgHandler(new HttpMessageConsumer(){

                @Override
                public boolean newHttpMessage(final HttpMessage msg, boolean consumed) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.add(msg);
                        }
                    });

                    return !msg.isClientRequest();
                }
            });
        }
        int serverPort = 8080;
        server.start(serverPort);
    }

    private void stopServer() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        server.stop();
    }
}
