package com.davidkubat.osmz;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.davidkubat.osmz.HttpServer.HttpMessage;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1234;
    MessageAdapter adapter;
    ListView messageList;
    Button startButton;
    Button stopButton;

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
        if (HttpServerService.messageQueue != null) {
            while (HttpServerService.messageQueue.peek() != null) {
                adapter.add(HttpServerService.messageQueue.poll());
            }
        }
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

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                while (HttpServerService.messageQueue.peek() != null) {
                    adapter.add(HttpServerService.messageQueue.poll());
                }
            }
        }, new IntentFilter(HttpServerService.HTTP_MESSAGE));
        checkServer();
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

    private void checkServer() {
        Intent mServiceIntent = new Intent(this, HttpServerService.class);
        if (isMyServiceRunning(HttpServerService.class)) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startServer() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        Intent mServiceIntent = new Intent(this, HttpServerService.class);
        startService(mServiceIntent);
    }

    private void stopServer() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        Intent mServiceIntent = new Intent(this, HttpServerService.class);
        stopService(mServiceIntent);
    }
}
