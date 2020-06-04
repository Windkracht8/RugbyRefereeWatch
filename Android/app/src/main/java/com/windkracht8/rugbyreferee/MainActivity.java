package com.windkracht8.rugbyreferee;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static MainActivity ma;//TODO: fix "memory leak"
    private TextView tvStatus;
    private TextView tvError;
    private Button bConnect;
    private Button bGetMatch;
    private tlMatch match;
    private tlSettings settings;
    private Button bSetSettings;
    private communication comms;//TODO: thread local?
    private boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ma = this;
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvError = findViewById(R.id.tvError);
        bConnect = findViewById(R.id.bConnect);
        bGetMatch = findViewById(R.id.bGetMatch);
        match = findViewById(R.id.tlMatch);
        settings = findViewById(R.id.tlSettings);
        bSetSettings = findViewById(R.id.bSetSettings);

    }

    @Override
    protected void onDestroy(){
        if(mIsBound){unbindService(mConnection);}
        ma = null;
        super.onDestroy();
    }

    private final ServiceConnection mConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            Log.i("MainActivity", "mConnection.onServiceConnected");
            comms = ((communication.LocalBinder) service).getService();
            comms.findPeers();
        }

        @Override
        public void onServiceDisconnected(ComponentName className){
            Log.i("MainActivity", "mConnection.onServiceDisconnected");
            mIsBound = false;
        }
    };

    public void bConnectClick(View view) {
        //bGetMatch.setVisibility(View.VISIBLE);
        //if(1==1)return;

        bConnect.setVisibility(View.GONE);
        if(comms == null || comms.status == communication.Status.DISCONNECTED) {
            mIsBound = bindService(new Intent(MainActivity.this, communication.class), mConnection, Context.BIND_AUTO_CREATE);
        }else if(comms.status == communication.Status.CONNECTION_LOST) {
            comms.findPeers();
        }else if(comms.status == communication.Status.ERROR) {
            comms.findPeers();
        }
    }
    public void bGetMatchClick(View view) {
        /*
        String debugResponse = "{\"requestType\":\"getMatch\",\"responseData\":{\"settings\":{\"split_time\":40,\"split_count\":2,\"sinbin\":10,\"points_try\":5,\"points_con\":2,\"points_goal\":3},\"home\":{\"team\":\"home\",\"tot\":5,\"trys\":1,\"cons\":0,\"goals\":0,\"color\":\"green\",\"sinbins\":[]},\"away\":{\"team\":\"away\",\"tot\":3,\"trys\":0,\"cons\":0,\"goals\":1,\"color\":\"red\",\"sinbins\":[{\"end\":592505,\"ended\":false,\"hide\":false}]},\"events\":[{\"time\":\"20:39:22\",\"timer\":\"0:00\",\"what\":\"Start split 1\"},{\"time\":\"20:39:24\",\"timer\":\"0:01\",\"team\":\"home\",\"what\":\"TRY\"},{\"time\":\"20:39:34\",\"timer\":\"0:11\",\"team\":\"away\",\"who\":\"9\",\"what\":\"yellow card\"},{\"time\":\"20:39:38\",\"timer\":\"0:13\",\"what\":\"Pause start\"},{\"time\":\"20:39:38\",\"timer\":\"0:13\",\"what\":\"Result after split 1: 5:0\"},{\"time\":\"20:39:40\",\"timer\":\"0:00\",\"what\":\"Pause over\"},{\"time\":\"20:39:41\",\"timer\":\"40:00\",\"what\":\"Start split 2\"},{\"time\":\"20:39:45\",\"timer\":\"40:03\",\"team\":\"away\",\"what\":\"PENALTY\"},{\"time\":\"20:39:47\",\"timer\":\"40:05\",\"what\":\"Pause start\"},{\"time\":\"20:39:47\",\"timer\":\"40:05\",\"what\":\"Result after split 2: 5:3\"}]}}";
        try {
            JSONObject responseMessage = new JSONObject(debugResponse);
            gotResponse(responseMessage.getString("requestType"),
                    responseMessage.getJSONObject("responseData"));
        }catch(Exception e){
            gotError("debug json error: " + e.getMessage());
        }
        if(1==1)return;
        */

        if(comms.status != communication.Status.CONNECTED){return;}
        comms.sendRequest("getMatch", null);
    }
    public void bSetSettingsClick(View view) {
        //Log.i("MainActivity.bSetSettingsClick", settings.getSettings().toString());
        //if(1==1)return;

        if(comms.status != communication.Status.CONNECTED){return;}
        comms.sendRequest("setSettings", settings.getSettings());
    }
    public static void updateStatus() {
        String status;
        switch(ma.comms.status){
            case DISCONNECTED:
                status = "Disconnected";
                break;
            case FINDING_PEERS:
                status = "Connecting";
                break;
            case CONNECTION_LOST:
                status = "Conection lost";
                ma.bConnect.setVisibility(View.VISIBLE);
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bSetSettings.setVisibility(View.GONE);
                break;
            case CONNECTED:
                status = "Connected";
                ma.bGetMatch.setVisibility(View.VISIBLE);
                break;
            case GETTING_MATCH:
                status = "Getting match";
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bSetSettings.setVisibility(View.GONE);
                break;
            case GETTING_SETTINGS:
                status = "Getting settings";
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bSetSettings.setVisibility(View.GONE);
                break;
            case SETTING_SETTINGS:
                status = "Sending settings";
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bSetSettings.setVisibility(View.GONE);
                break;
            case ERROR:
            default:
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bSetSettings.setVisibility(View.GONE);
                status = "error";
        }
        ma.tvStatus.setText(status);
    }
    public static void gotResponse(final JSONObject responseMessage){
        Log.i("MainActivity.gotResult", responseMessage.toString());
        try {
            String requestType = responseMessage.getString("requestType");
            switch (requestType) {
                case "getMatch":
                    Log.i("MainActivity.gotResult", "GET_MATCH");
                    JSONObject responseData = responseMessage.getJSONObject("responseData");
                    ma.match.gotMatch(responseData);
                    ma.settings.gotMatch(responseData);
                    ma.bSetSettings.setVisibility(View.VISIBLE);
                    break;
                case "setSettings":
                    Log.i("MainActivity.gotResult", "SET_SETTINGS");
                    String responseString = responseMessage.getString("responseData");
                    if (!responseString.equals("okilly dokilly")) {
                        gotError(responseString);
                    }
                    break;
            }
        }catch(Exception e){
            gotError("gotResponse exception: " + e.getMessage());
        }
    }
    public static void gotError(String error) {
        Log.i("MainActivity.gotError", error);
        ma.tvError.setText(error);
    }
}
