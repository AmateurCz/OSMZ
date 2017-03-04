package com.davidkubat.osmz;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.davidkubat.osmz.HttpServer.HttpMessage;

import java.util.List;

public class MessageAdapter extends ArrayAdapter<HttpMessage> {

    java.text.SimpleDateFormat format;
    LayoutInflater mInflater;

    public MessageAdapter(Context context, List<HttpMessage> objects) {
        super(context, R.layout.message, objects);
        format = new java.text.SimpleDateFormat("HH:mm:ss:SSS");
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.message, null);
        }
        HttpMessage msg = getItem(position);
        TextView dView = (TextView)convertView.findViewById(R.id.message_date);
        TextView sView = (TextView)convertView.findViewById(R.id.message_source);
        TextView tView = (TextView)convertView.findViewById(R.id.message_text);
        int color = Color.BLACK;
        dView.setText(format.format(msg.createdAt()));
        sView.setText(msg.getSource());
        switch (msg.getType()){
            case ServerStart:
                color = Color.argb(255,0,255,0);
                tView.setText("Server started");
                break;

            case ServerStop:
                color = Color.argb(255,50,50,50);
                tView.setText("Server stoped");
                break;

            case ERROR:
                color = Color.argb(255,255,0,0);
                tView.setText("Server encountered error");
                break;

            case GET:
                color = getColorFromSource(msg.getSource());
                tView.setText("GET " + msg.getDesiredObject());
                break;
        }
        dView.setTextColor(color);
        tView.setTextColor(color);
        sView.setTextColor(color);
        return convertView;
    }

    public int getColorFromSource(String source) {
        return Color.argb(255,0,0,0);
    }
}
