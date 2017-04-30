package com.davidkubat.osmz;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.davidkubat.osmz.HttpServer.CgiBinResponder;
import com.davidkubat.osmz.HttpServer.ErrorResponder;
import com.davidkubat.osmz.HttpServer.FileBrowserResponder;
import com.davidkubat.osmz.HttpServer.HTTPServer;
import com.davidkubat.osmz.HttpServer.HttpMessage;
import com.davidkubat.osmz.HttpServer.HttpMessageConsumer;
import com.davidkubat.osmz.HttpServer.PageResponder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1234;
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
        messageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                HttpMessage msg = adapter.getItem(position);
// 2. Chain together various setter methods to set the dialog characteristics

                String detailText;
                if (msg.getType() == HttpMessage.MsgType.ERROR) {
                    detailText = msg.getContent();
                } else {
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


        // Here, thisActivity is the current activity
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int permisionStatus = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permisionStatus != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.
                    try {

                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

                    } catch (Exception e) {
                        Log.e("WTF", e.getMessage());
                    }

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("SERVER", "External storage read permission granted");

                } else {
                    Log.e("SERVER", "External storage read permission DENIED");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause() {
        stopServer();
        super.onPause();
    }

    private void startServer() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        if (server != null && server.isRunning())
            throw new IllegalStateException("Server is already running");
        if (server == null) {
            server = new HTTPServer(this);
            server.addMsgHandler(new HttpMessageConsumer() {

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

            server.addMsgHandler(new CgiBinResponder(server));
            server.addMsgHandler(new FileBrowserResponder(server));
            server.addMsgHandler(new PageResponder(server));
            server.addMsgHandler(new ErrorResponder(server));
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
