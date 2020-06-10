package com.windkracht8.rugbyreferee;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
    @SuppressLint("StaticFieldLeak")
    private static MainActivity ma;
    private TextView tvStatus;
    private TextView tvError;
    private TextView tabReport;
    private TextView tabPrepare;
    private Button bConnect;
    private Button bExit;
    private Button bGetMatch;
    private report rReport;
    private prepare pPrepare;
    private Button bPrepare;
    private communication comms;
    private boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ma = this;
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvError = findViewById(R.id.tvError);
        tabReport = findViewById(R.id.tabReport);
        tabPrepare = findViewById(R.id.tabPrepare);
        bConnect = findViewById(R.id.bConnect);
        bExit = findViewById(R.id.bExit);
        bGetMatch = findViewById(R.id.bGetMatch);
        rReport = findViewById(R.id.report);
        pPrepare = findViewById(R.id.prepare);
        bPrepare = findViewById(R.id.bPrepare);
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

    public void bExitClick(View view) {
        finish();
        System.exit(0);
    }
    public void bConnectClick(View view) {
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
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError("First connect with watch");return;}
        comms.sendRequest("getMatch", null);
    }
    public void bSetSettingsClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError("First connect with watch");return;}
        comms.sendRequest("prepare", pPrepare.getSettings());
    }
    public static void updateStatus() {
        String status;
        ma.tvError.setText("");
        switch(ma.comms.status){
            case DISCONNECTED:
                status = "Disconnected";
                break;
            case FINDING_PEERS:
                status = "Connecting";
                break;
            case CONNECTION_LOST:
                status = "Connection lost";
                ma.bConnect.setVisibility(View.VISIBLE);
                ma.bExit.setVisibility(View.GONE);
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case CONNECTED:
                ma.tvError.setText("");
                status = "Connected";
                ma.bExit.setVisibility(View.VISIBLE);
                ma.bGetMatch.setVisibility(View.VISIBLE);
                ma.bPrepare.setVisibility(View.VISIBLE);
                break;
            case GETTING_MATCH:
                status = "Getting match";
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case PREPARE:
                status = "Sending prepare";
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case ERROR:
            default:
                status = "error";
                ma.bConnect.setVisibility(View.VISIBLE);
                ma.bExit.setVisibility(View.GONE);
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
        }
        ma.tvStatus.setText(status);
    }
    public static void gotResponse(final JSONObject responseMessage){
        Log.i("MainActivity.gotResponse", responseMessage.toString());
        try {
            String requestType = responseMessage.getString("requestType");
            switch (requestType) {
                case "getMatch":
                    Log.i("MainActivity.gotResult", "getMatch");
                    JSONObject getMatchResponse = responseMessage.getJSONObject("responseData");
                    ma.rReport.gotMatch(getMatchResponse);
                    break;
                case "prepare":
                    Log.i("MainActivity.gotResult", "prepare");
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

    public void tabReportClick(View view) {
        tabReport.setBackgroundResource(R.drawable.tab_active);
        tabPrepare.setBackgroundResource(0);
        rReport.setVisibility(View.VISIBLE);
        pPrepare.setVisibility(View.GONE);
    }

    public void tabPrepareClick(View view) {
        tabReport.setBackgroundResource(0);
        tabPrepare.setBackgroundResource(R.drawable.tab_active);
        rReport.setVisibility(View.GONE);
        pPrepare.setVisibility(View.VISIBLE);
    }
}
