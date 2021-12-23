package com.windkracht8.rugbyrefereewatch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.samsung.android.sdk.accessory.SAAgentV2;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
//TODO: improve the top layout
public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private communication_tizen comms_tizen = null;
    private communication_wear comms_wear = null;

    private long backpresstime;
    private BroadcastReceiver rrwReceiver;
    private boolean tizenNotWear = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        setContentView(R.layout.activity_main);
        findViewById(R.id.bConnect).setOnClickListener(view -> bConnectClick());
        findViewById(R.id.bExit).setOnClickListener(view -> bExitClick());
        findViewById(R.id.tabHistory).setOnClickListener(view -> tabHistoryClick());
        findViewById(R.id.tabReport).setOnClickListener(view -> tabReportClick());
        findViewById(R.id.tabPrepare).setOnClickListener(view -> tabPrepareClick());
        findViewById(R.id.bGetMatches).setOnClickListener(view -> bGetMatchesClick());
        findViewById(R.id.bGetMatch).setOnClickListener(view -> bGetMatchClick());
        findViewById(R.id.bPrepare).setOnClickListener(view -> bPrepareClick());

        handleOrientation();

        SharedPreferences sharedpreferences = getSharedPreferences("com.windkracht8.rrw.prefs", Context.MODE_PRIVATE);
        if(sharedpreferences.getBoolean("firstboot", true)){
            tizenNotWear = guessTizenNotWear();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean("firstboot", false);
            editor.apply();
        }else {
            tizenNotWear = sharedpreferences.getBoolean("tizenNotWear", false);
        }

        Spinner sOS = findViewById(R.id.sOS);
        int[] icons = {R.drawable.os_tizen, R.drawable.os_wear};
        int[] names = {R.string.os_tizen, R.string.os_wear};
        osAdapter osa = new osAdapter(getApplicationContext(), icons, names);
        sOS.setAdapter(osa);

        sOS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switchOS(pos == 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        if(tizenNotWear){
            sOS.setSelection(0);
            initTizen();
        }else{
            sOS.setSelection(1);
            initWear();
        }

        rrwReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!intent.hasExtra("intentType")){return;}
                switch(intent.getStringExtra("intentType")){
                    case "gotError":
                        if(!intent.hasExtra("error")){return;}
                        gotError(intent.getStringExtra("error"));
                        break;
                    case "gotResponse":
                        if(!intent.hasExtra("requestType")){return;}
                        if(!intent.hasExtra("responseData")){return;}
                        gotResponse(intent.getStringExtra("requestType"), intent.getStringExtra("responseData"));
                    case "updateStatus":
                        if(!intent.hasExtra("newstatus")){return;}
                        updateStatus(intent.getStringExtra("newstatus"));
                        break;
                    case "historyMatchClick":
                        if(!intent.hasExtra("match")){return;}
                        historyMatchClick(intent.getStringExtra("match"));
                        break;
                    case "updateTeamName":
                        if(!intent.hasExtra("name")){return;}
                        if(!intent.hasExtra("teamid")){return;}
                        if(!intent.hasExtra("matchid")){return;}
                        updateTeamName(intent.getStringExtra("name"),
                                intent.getStringExtra("teamid"),
                                intent.getLongExtra("matchid", 0));
                        break;
                    case "updateCardReason":
                        if(!intent.hasExtra("match")){return;}
                        if(!intent.hasExtra("matchid")){return;}
                        if(!intent.hasExtra("eventid")){return;}
                        updateCardReason(intent.getStringExtra("match"),
                                intent.getLongExtra("matchid", 0),
                                intent.getLongExtra("eventid", 0));
                        break;
                    case "exportMatches":
                        exportMatches();
                        break;
                }
            }
        };
        registerReceiver(rrwReceiver, new IntentFilter("com.windkracht8.rugbyrefereewatch"));

        handleIntent();
    }
    private boolean guessTizenNotWear(){
        try{
            getPackageManager().getPackageInfo("com.samsung.accessory", PackageManager.GET_ACTIVITIES);
            return true;
        }catch(PackageManager.NameNotFoundException e){
            return false;
        }
    }
    private void switchOS(boolean tizenNotWear){
        if(this.tizenNotWear == tizenNotWear)return;
        findViewById(R.id.bConnect).setVisibility(View.GONE);
        findViewById(R.id.bExit).setVisibility(View.GONE);
        findViewById(R.id.bGetMatches).setVisibility(View.GONE);
        findViewById(R.id.bGetMatch).setVisibility(View.GONE);
        findViewById(R.id.bPrepare).setVisibility(View.GONE);
        updateStatus("DISCONNECTED");
        if(this.tizenNotWear){
            destroyTizen();
            initWear();
        }else{
            destroyWear();
            initTizen();
        }
        this.tizenNotWear = tizenNotWear;
        SharedPreferences sharedpreferences = getSharedPreferences("com.windkracht8.rrw.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("tizenNotWear", tizenNotWear);
        editor.apply();
    }
    private void initTizen(){
        try{
            getPackageManager().getPackageInfo("com.samsung.accessory", PackageManager.GET_ACTIVITIES);
            SAAgentV2.requestAgent(getApplicationContext(), communication_tizen.class.getName(), mAgentCallback1);
            findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
            updateStatus("DISCONNECTED");
        }catch(PackageManager.NameNotFoundException e){
            findViewById(R.id.tvStatus).setVisibility(View.GONE);
            TextView tvError = findViewById(R.id.tvError);
            tvError.setText(R.string.tvFatal);
            findViewById(R.id.tvError).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.samsung.accessory"))));
        }
    }
    private void destroyTizen(){
        if (comms_tizen != null) {
            comms_tizen.closeConnection();
            comms_tizen.releaseAgent();
            comms_tizen = null;
        }
    }
    private void initWear(){
        comms_wear = new communication_wear(getApplicationContext());
        findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
        findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
        findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
    }
    private void destroyWear(){
        if (comms_wear != null) {
            comms_wear.stop();
            comms_wear = null;
        }
    }
    private void handleIntent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.compareTo(Intent.ACTION_VIEW) != 0) return;

        String scheme = intent.getScheme();
        if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) != 0) {
            Log.e("MainActivity" , "Non supported scheme: " + scheme);
            return;
        }
        try {
            ContentResolver cr = getContentResolver();
            InputStream is = cr.openInputStream(intent.getData());
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder text = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
            String sNewmatches = text.toString();
            JSONArray newmatches = new JSONArray(sNewmatches);
            history hHistory = findViewById(R.id.hHistory);
            hHistory.gotMatches(newmatches);
        } catch (Exception e) {
            Log.e("MainActivity", "handleIntent read file: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(rrwReceiver);
        destroyTizen();
        destroyWear();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleOrientation();
    }

    private void handleOrientation(){
        ImageView ivIcon = findViewById(R.id.ivIcon);
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
            bExitClick();
        }
        backpresstime = date.getTime();
        if(findViewById(R.id.hHistory).getVisibility() == View.VISIBLE){
            history hHistory = findViewById(R.id.hHistory);
            hHistory.unselect();
        }else if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabHistoryClick();
        }else if(findViewById(R.id.pPrepare).getVisibility() == View.VISIBLE){
            tabReportClick();
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
        if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabHistoryClick();
        }else if(findViewById(R.id.pPrepare).getVisibility() == View.VISIBLE){
            tabReportClick();
        }
    }

    public void onSwipeLeft() {
        if(findViewById(R.id.hHistory).getVisibility() == View.VISIBLE){
            tabReportClick();
        }else if(findViewById(R.id.rReport).getVisibility() == View.VISIBLE){
            tabPrepareClick();
        }
    }

    private final SAAgentV2.RequestAgentCallback mAgentCallback1 = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            comms_tizen = (communication_tizen) agent;
            findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            Log.e("MainActivity", "Agent initialization error: " + errorCode + ". ErrorMsg: " + errorMessage);
            gotError(errorMessage);
        }
    };

    public void bExitClick() {
        finish();
        System.exit(0);
    }

    public void bConnectClick() {
        gotError("");
        findViewById(R.id.bConnect).setVisibility(View.GONE);
        if(comms_tizen == null) {
            gotError("Watch not found");
            return;
        }
        if(comms_tizen.status.equals("DISCONNECTED") ||
           comms_tizen.status.equals("CONNECTION_LOST") ||
           comms_tizen.status.equals("ERROR")
        ) {
            comms_tizen.findPeers();
        }
    }
    public void bGetMatchesClick() {
        if(cantSendRequest()){return;}
        gotError("");
        history hHistory = findViewById(R.id.hHistory);
        sendRequest( "getMatches", hHistory.getDeletedMatches());
    }
    public void bGetMatchClick() {
        if(cantSendRequest()){return;}
        gotError("");
        sendRequest("getMatch", null);
    }
    public void bPrepareClick() {
        if(cantSendRequest()){return;}
        gotError("");
        prepare pPrepare = findViewById(R.id.pPrepare);
        JSONObject requestData = pPrepare.getSettings();
        if(requestData == null){
            gotError("Error with settings");
            return;
        }
        sendRequest("prepare", requestData);
    }
    private void sendRequest(String requestType, JSONObject requestData){
        if(tizenNotWear) {
            comms_tizen.sendRequest(requestType, requestData);
        }else {
            communication_wear.sendRequest(this, requestType, requestData);
        }
    }
    private boolean cantSendRequest(){
        if(tizenNotWear && (comms_tizen == null || !comms_tizen.status.equals("CONNECTED"))){
            gotError(getString(R.string.first_connect));
            return true;
        }
        if(!tizenNotWear && !comms_wear.status.equals("CONNECTED")){
            gotError(getString(R.string.first_connect));
            return true;
        }
        return false;
    }
    private void historyMatchClick(String match) {
        try{
            JSONObject match_json = new JSONObject(match);
            report rReport = findViewById(R.id.rReport);
            rReport.gotMatch(match_json);
            tabReportClick();
        } catch (Exception e) {
            gotError("Issue with match: " + e.getMessage());
        }
    }

    public void updateStatus(final String newstatus) {
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvError = findViewById(R.id.tvError);
        tvError.setText("");

        String status;
        switch(newstatus){
            case "FATAL":
                findViewById(R.id.bConnect).setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                return;
            case "DISCONNECTED":
                status = getString(R.string.status_DISCONNECTED);
                break;
            case "FINDING_PEERS":
                status = getString(R.string.status_CONNECTING);
                break;
            case "CONNECTION_LOST":
                status = getString(R.string.status_CONNECTION_LOST);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bExit).setVisibility(View.GONE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                break;
            case "CONNECTED":
                status = getString(R.string.status_CONNECTED);
                findViewById(R.id.bExit).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
                break;
            case "OFFLINE":
                status = getString(R.string.status_OFFLINE);
                findViewById(R.id.bExit).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
                break;
            case "GETTING_MATCHES":
                status = getString(R.string.status_GETTING_MATCHES);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case "GETTING_MATCH":
                status = getString(R.string.status_GETTING_MATCH);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case "PREPARE":
                status = getString(R.string.status_PREPARE);
                findViewById(R.id.bGetMatches).setVisibility(View.INVISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.INVISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.INVISIBLE);
                break;
            case "ERROR":
            default:
                status = getString(R.string.status_ERROR);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bExit).setVisibility(View.GONE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
        }
        tvStatus.setText(status);
    }

    public void gotResponse(final String requestType, final String responseData){
        try {
            switch (requestType) {
                case "getMatches":
                    Log.i("MainActivity.gotResult", "getMatches");
                    JSONArray getMatchesResponse = new JSONArray(responseData);
                    history hHistory = findViewById(R.id.hHistory);
                    hHistory.gotMatches(getMatchesResponse);
                    break;
                case "getMatch":
                    Log.i("MainActivity.gotResult", "getMatch");
                    JSONObject getMatchResponse = new JSONObject(responseData);
                    report rReport = findViewById(R.id.rReport);
                    rReport.gotMatch(getMatchResponse);
                    break;
                case "prepare":
                    Log.i("MainActivity.gotResult", "prepare");
                    if (!responseData.equals("okilly dokilly")) {
                        gotError(responseData);
                    }
                    break;
            }
        }catch(Exception e){
            gotError("gotResponse exception: " + e.getMessage());
        }
    }
    public void gotError(String error) {
        Log.i("MainActivity.gotError", error);
        if(error.equals(getString(R.string.did_not_understand_message))) {
            error = getString(R.string.update_watch_app);
        }
        TextView tvError = findViewById(R.id.tvError);
        tvError.setText(error);
    }

    public void tabHistoryClick() {
        findViewById(R.id.tabHistory).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabReport).setBackgroundResource(0);
        findViewById(R.id.tabPrepare).setBackgroundResource(0);
        findViewById(R.id.hHistory).setVisibility(View.VISIBLE);
        findViewById(R.id.rReport).setVisibility(View.GONE);
        findViewById(R.id.pPrepare).setVisibility(View.GONE);
    }
    public void tabReportClick() {
        findViewById(R.id.tabHistory).setBackgroundResource(0);
        findViewById(R.id.tabReport).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabPrepare).setBackgroundResource(0);
        findViewById(R.id.hHistory).setVisibility(View.GONE);
        findViewById(R.id.rReport).setVisibility(View.VISIBLE);
        findViewById(R.id.pPrepare).setVisibility(View.GONE);
    }
    public void tabPrepareClick() {
        findViewById(R.id.tabHistory).setBackgroundResource(0);
        findViewById(R.id.tabReport).setBackgroundResource(0);
        findViewById(R.id.tabPrepare).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.hHistory).setVisibility(View.GONE);
        findViewById(R.id.rReport).setVisibility(View.GONE);
        findViewById(R.id.pPrepare).setVisibility(View.VISIBLE);
    }

    private void updateCardReason(String reason, long matchid, long eventid){
        history hHistory = findViewById(R.id.hHistory);
        hHistory.updateCardReason(reason, matchid, eventid);
    }
    private void updateTeamName(String name, String teamid, long matchid){
        history hHistory = findViewById(R.id.hHistory);
        hHistory.updateTeamName(name, teamid, matchid);
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

    private void exportMatches() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "matches.json");

        exportMatchesActivityResultLauncher.launch(intent);
    }
    ActivityResultLauncher<Intent> exportMatchesActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        OutputStream outputStream;
                        try {
                            outputStream = getContentResolver().openOutputStream(uri);
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
                            history hHistory = findViewById(R.id.hHistory);
                            bw.write(hHistory.export_matches.toString());
                            bw.flush();
                            bw.close();
                        } catch (Exception e) {
                            Log.e("MainActivity", "onActivityResult: " + e.getMessage());
                        }
                    }
                }
            });
}
