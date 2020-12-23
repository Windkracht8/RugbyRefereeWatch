package com.windkracht8.rugbyrefereewatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.android.sdk.accessory.SAAgentV2;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static Handler mainThreadHandler;
    private GestureDetector gestureDetector;
    @SuppressLint("StaticFieldLeak")
    private static MainActivity ma;
    private ImageView ivIcon;
    private TextView tvStatus;
    private TextView tvError;
    private TextView tabHistory;
    private TextView tabReport;
    private TextView tabPrepare;
    private Button bConnect;
    private Button bExit;
    private history hHistory;
    private Button bGetMatches;

    private report rReport;
    private Button bGetMatch;

    private prepare pPrepare;
    private Button bPrepare;
    private static communication comms = null;

    private long backpresstime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainThreadHandler = new Handler();
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        ma = this;
        setContentView(R.layout.activity_main);

        ivIcon = findViewById(R.id.ivIcon);
        tvStatus = findViewById(R.id.tvStatus);
        tvError = findViewById(R.id.tvError);
        tabHistory = findViewById(R.id.tabHistory);
        tabReport = findViewById(R.id.tabReport);
        tabPrepare = findViewById(R.id.tabPrepare);
        bConnect = findViewById(R.id.bConnect);
        bExit = findViewById(R.id.bExit);

        hHistory = findViewById(R.id.hHistory);
        bGetMatches = findViewById(R.id.bGetMatches);
        rReport = findViewById(R.id.rReport);
        bGetMatch = findViewById(R.id.bGetMatch);
        pPrepare = findViewById(R.id.pPrepare);
        bPrepare = findViewById(R.id.bPrepare);

        handleOrientation();
        try{
            getPackageManager().getPackageInfo("com.samsung.accessory", PackageManager.GET_ACTIVITIES);
            SAAgentV2.requestAgent(getApplicationContext(), communication.class.getName(), mAgentCallback1);
            bConnect.setVisibility(View.VISIBLE);
        }catch(PackageManager.NameNotFoundException e){
            tvStatus.setVisibility(View.GONE);
            tvError.setText(R.string.tvFatal);
        }
    }

    @Override
    protected void onDestroy(){
        if (comms != null) {
            comms.closeConnection();
            comms.releaseAgent();
            comms = null;
        }
        ma = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleOrientation();
    }

    private void handleOrientation(){
        Resources r = getResources();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
        }else {
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
        }
    }
    @Override
    public void onBackPressed() {
        Date date = new Date();
        if(date.getTime() - backpresstime < 1000){
            bExitClick(null);
        }
        backpresstime = date.getTime();
        if(hHistory.getVisibility() == View.VISIBLE){
            hHistory.unselect();
        }else if(rReport.getVisibility() == View.VISIBLE){
            tabHistoryClick(null);
        }else if(pPrepare.getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
            }catch(Exception e){
                Log.e("MainActivity", "onFling: " + e.getMessage());
            }
            return result;
        }
    }

    public void onSwipeRight() {
        if(rReport.getVisibility() == View.VISIBLE){
            tabHistoryClick(null);
        }else if(pPrepare.getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }
    }

    public void onSwipeLeft() {
        if(hHistory.getVisibility() == View.VISIBLE){
            tabReportClick(null);
        }else if(rReport.getVisibility() == View.VISIBLE){
            tabPrepareClick(null);
        }
    }

    private final SAAgentV2.RequestAgentCallback mAgentCallback1 = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            comms = (communication) agent;
            bConnect.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            Log.e("MainActivity", "Agent initialization error: " + errorCode + ". ErrorMsg: " + errorMessage);
            gotError(errorMessage);
        }
    };

    public void bExitClick(View view) {
        finish();
        System.exit(0);
    }
    public void bConnectClick(View view) {
        gotError("");
        bConnect.setVisibility(View.GONE);
        if(comms == null) {
            gotError("Watch not found");
            return;
        }
        if(comms.status == communication.Status.DISCONNECTED ||
            comms.status == communication.Status.CONNECTION_LOST ||
            comms.status == communication.Status.ERROR
        ) {
            comms.findPeers();
        }
    }
    public void bGetMatchesClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        comms.sendRequest("getMatches", hHistory.getDeletedMatches());
    }
    public void bGetMatchClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        comms.sendRequest("getMatch", null);
    }
    public void bPrepareClick(View view) {
        if(comms == null || comms.status != communication.Status.CONNECTED){gotError(getString(R.string.first_connect));return;}
        gotError("");
        JSONObject requestData = pPrepare.getSettings();
        if(requestData == null){
            gotError("Error with settings");
            return;
        }
        comms.sendRequest("prepare", requestData);
    }
    public static void historyMatchClick(JSONObject match) {
        ma.rReport.gotMatch(match);
        ma.tabReportClick(null);
    }

    public static void updateStatus(final communication.Status newstatus) {
        String status;
        ma.tvError.setText("");
        gotError("");
        switch(newstatus){
            case FATAL:
                ma.bConnect.setVisibility(View.GONE);
                ma.tvStatus.setVisibility(View.GONE);
                return;
            case DISCONNECTED:
                status = ma.getString(R.string.status_DISCONNECTED);
                break;
            case FINDING_PEERS:
                status = ma.getString(R.string.status_CONNECTING);
                break;
            case CONNECTION_LOST:
                status = ma.getString(R.string.status_CONNECTION_LOST);
                ma.bConnect.setVisibility(View.VISIBLE);
                ma.bExit.setVisibility(View.GONE);
                ma.bGetMatches.setVisibility(View.GONE);
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bPrepare.setVisibility(View.GONE);
                break;
            case CONNECTED:
                ma.tvError.setText("");
                status = ma.getString(R.string.status_CONNECTED);
                ma.bExit.setVisibility(View.VISIBLE);
                ma.bGetMatches.setVisibility(View.VISIBLE);
                ma.bGetMatch.setVisibility(View.VISIBLE);
                ma.bPrepare.setVisibility(View.VISIBLE);
                break;
            case GETTING_MATCHES:
                status = ma.getString(R.string.status_GETTING_MATCHES);
                ma.bGetMatches.setVisibility(View.INVISIBLE);
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case GETTING_MATCH:
                status = ma.getString(R.string.status_GETTING_MATCH);
                ma.bGetMatches.setVisibility(View.INVISIBLE);
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case PREPARE:
                status = ma.getString(R.string.status_PREPARE);
                ma.bGetMatches.setVisibility(View.INVISIBLE);
                ma.bGetMatch.setVisibility(View.INVISIBLE);
                ma.bPrepare.setVisibility(View.INVISIBLE);
                break;
            case ERROR:
            default:
                status = ma.getString(R.string.status_ERROR);
                ma.bConnect.setVisibility(View.VISIBLE);
                ma.bExit.setVisibility(View.GONE);
                ma.bGetMatches.setVisibility(View.GONE);
                ma.bGetMatch.setVisibility(View.GONE);
                ma.bPrepare.setVisibility(View.GONE);
        }
        ma.tvStatus.setText(status);
    }
    public static void gotResponse(final JSONObject responseMessage){
        Log.i("MainActivity.gotResponse", responseMessage.toString());
        try {
            String requestType = responseMessage.getString("requestType");
            switch (requestType) {
                case "getMatches":
                    Log.i("MainActivity.gotResult", "getMatches");
                    JSONArray getMatchesResponse = responseMessage.getJSONArray("responseData");
                    ma.hHistory.gotMatches(getMatchesResponse);
                    break;
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
        if(error.equals(ma.getString(R.string.did_not_understand_message))) {
            error = ma.getString(R.string.update_watch_app);
        }
        ma.tvError.setText(error);
    }

    public void tabHistoryClick(View view) {
        tabHistory.setBackgroundResource(R.drawable.tab_active);
        tabReport.setBackgroundResource(0);
        tabPrepare.setBackgroundResource(0);
        hHistory.setVisibility(View.VISIBLE);
        rReport.setVisibility(View.GONE);
        pPrepare.setVisibility(View.GONE);
    }
    public void tabReportClick(View view) {
        tabHistory.setBackgroundResource(0);
        tabReport.setBackgroundResource(R.drawable.tab_active);
        tabPrepare.setBackgroundResource(0);
        hHistory.setVisibility(View.GONE);
        rReport.setVisibility(View.VISIBLE);
        pPrepare.setVisibility(View.GONE);
    }
    public void tabPrepareClick(View view) {
        tabHistory.setBackgroundResource(0);
        tabReport.setBackgroundResource(0);
        tabPrepare.setBackgroundResource(R.drawable.tab_active);
        hHistory.setVisibility(View.GONE);
        rReport.setVisibility(View.GONE);
        pPrepare.setVisibility(View.VISIBLE);
    }

    public static void updateCardReason(String reason, long matchid, long eventid){
        ma.hHistory.updateCardReason(reason, matchid, eventid);
    }
    public static void updateTeamName(String name, String teamid, long matchid){
        ma.hHistory.updateTeamName(name, teamid, matchid);
    }

    public static String getTeamName(JSONObject team) {
        String name = "";
        try {
            name = team.getString("team");
            if (team.has("id") && !team.getString("id").equals(name)) {
                return name;
            }
            String color = team.getString("color");
            color = color.equals("lightgray") ? "white" : color;
            return name + " (" + color + ")";
        } catch (Exception e) {
            Log.e("MainActivity", "getTeamName: " + e.getMessage());
        }
        return name;
    }
}
