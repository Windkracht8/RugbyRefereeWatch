package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

public class DeviceConnect extends Activity implements CommsBT.BTInterface{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_connect);
        findViewById(android.R.id.content).setOnApplyWindowInsetsListener(Main.onApplyWindowInsetsListener);
        String name = getIntent().getStringExtra("name");
        if(name == null) ((TextView)findViewById(R.id.device_connect_name)).setText(R.string.connecting_to);
        else ((TextView)findViewById(R.id.device_connect_name)).setText(getString(R.string.connecting_to, name));

        ImageView icon = findViewById(R.id.device_connect_icon);
        ((AnimatedVectorDrawable) icon.getBackground()).start();

        try{
            Main.commsBT.addListener(this);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "DeviceConnect.onCreate Failed to add as a listener: " + e.getMessage());
        }
    }
    @Override
    public void onBTStartDone(){finishAndRemoveTask();}
    @Override
    public void onBTConnecting(String x){}
    @Override
    public void onBTConnectFailed(){finishAndRemoveTask();}
    @Override
    public void onBTConnected(String x){finishAndRemoveTask();}
    @Override
    public void onBTDisconnected(){}
    @Override
    public void onBTResponse(JSONObject x){}
    @Override
    public void onBTError(int x){}

}
