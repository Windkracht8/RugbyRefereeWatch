package com.windkracht8.rugbyrefereewatch;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONObject;

public class DeviceConnect extends FragmentActivity implements CommsBT.BTInterface{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_connect);
        Intent startDeviceConnect = getIntent();
        String text = getString(R.string.connecting_to) + " " + startDeviceConnect.getStringExtra("name");
        ((TextView)findViewById(R.id.device_connect_name)).setText(text);

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
