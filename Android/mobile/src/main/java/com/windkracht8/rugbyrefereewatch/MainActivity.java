package com.windkracht8.rugbyrefereewatch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity{
    private static final String TIZEN_PACKAGE_NAME = "com.samsung.accessory";
    private static final String WEAR_PACKAGE_NAME = "com.google.android.wearable.app";
    private GestureDetector gestureDetector;
    private CommsTizen comms_tizen = null;
    private CommsWear comms_wear = null;
    private SharedPreferences.Editor sharedPreferences_editor;

    private long back_press_time;
    private BroadcastReceiver rrwReceiver;
    private boolean tizen_not_wear = true;
    private Handler handler_main;

    private TabHistory tabHistory;
    private TabReport tabReport;
    private TabPrepare tabPrepare;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        setContentView(R.layout.activity_main);
        tabHistory = findViewById(R.id.tabHistory);
        tabReport = findViewById(R.id.tabReport);
        tabPrepare = findViewById(R.id.tabPrepare);
        findViewById(R.id.bConnect).setOnClickListener(view -> bConnectClick());
        findViewById(R.id.bSearch).setOnClickListener(view -> bSearchClick());
        findViewById(R.id.tabHistoryLabel).setOnClickListener(view -> tabHistoryLabelClick());
        findViewById(R.id.tabReportLabel).setOnClickListener(view -> tabReportLabelClick());
        findViewById(R.id.tabPrepareLabel).setOnClickListener(view -> tabPrepareLabelClick());
        findViewById(R.id.bGetMatches).setOnClickListener(view -> bGetMatchesClick());
        findViewById(R.id.bGetMatch).setOnClickListener(view -> bGetMatchClick());
        findViewById(R.id.bPrepare).setOnClickListener(view -> bPrepareClick());

        handleOrientation();

        SharedPreferences sharedPreferences = getSharedPreferences("com.windkracht8.rugbyrefereewatch", Context.MODE_PRIVATE);
        sharedPreferences_editor = sharedPreferences.edit();
        if(!sharedPreferences.contains("tizen_not_wear")){
            tizen_not_wear = hasPackage(TIZEN_PACKAGE_NAME);
            sharedPreferences_editor.putBoolean("tizen_not_wear", tizen_not_wear);
            sharedPreferences_editor.apply();
        }else{
            tizen_not_wear = sharedPreferences.getBoolean("tizen_not_wear", true);
        }

        Spinner sOS = findViewById(R.id.sOS);
        osAdapter osa = new osAdapter(getApplicationContext());
        sOS.setAdapter(osa);
        sOS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                switchOS(pos == 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });
        if(tizen_not_wear){
            sOS.setSelection(0);
            initTizen();
        }else{
            sOS.setSelection(1);
            initWear();
        }

        rrwReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                if(!intent.hasExtra("intent_type")){return;}
                if(tizen_not_wear && intent.hasExtra("source") && intent.getStringExtra("source").equals("wear")){return;}
                if(!tizen_not_wear && intent.hasExtra("source") && intent.getStringExtra("source").equals("tizen")){return;}
                switch(intent.getStringExtra("intent_type")){
                    case "gotError":
                        if(!intent.hasExtra("error")){return;}
                        gotError(intent.getStringExtra("error"));
                        break;
                    case "gotResponse":
                        if(!intent.hasExtra("responseData") || !intent.hasExtra("requestType")){return;}
                        gotResponse(intent.getStringExtra("requestType"), intent.getStringExtra("responseData"));
                    case "updateStatus":
                        if(!intent.hasExtra("status_new")){return;}
                        updateStatus(intent.getStringExtra("status_new"));
                        break;
                    case "historyMatchClick":
                        if(!intent.hasExtra("match")){return;}
                        historyMatchClick(intent.getStringExtra("match"));
                        break;
                    case "bDelClick":
                        if(!intent.hasExtra("event_id")){return;}
                        tabReport.bDelClick(intent.getIntExtra("event_id", 0));
                        break;
                    case "updateMatch":
                        if(!intent.hasExtra("match")){return;}
                        tabHistory.updateMatch(intent.getStringExtra("match"));
                        break;
                    case "exportMatches":
                        exportMatches();
                        break;
                }
            }
        };
        registerReceiver(rrwReceiver, new IntentFilter("com.windkracht8.rugbyrefereewatch"));

        handleIntent();
        handler_main = new Handler(Looper.getMainLooper());

        findViewById(R.id.scrollHistory).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.llMatches).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.scrollReport).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.scrollPrepare).setOnTouchListener(this::onTouchEventScrollViews);
    }
    private boolean hasPackage(String packageName){
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                getPackageManager().getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
            }else{
                getPackageManager().getPackageInfo(packageName, 0);
            }
            return true;
        }catch(PackageManager.NameNotFoundException e){
            return false;
        }
    }
    private void switchOS(boolean tizenNotWear){
        if(this.tizen_not_wear == tizenNotWear) return;
        this.tizen_not_wear = tizenNotWear;
        findViewById(R.id.bConnect).setVisibility(View.GONE);
        findViewById(R.id.bGetMatches).setVisibility(View.GONE);
        findViewById(R.id.bGetMatch).setVisibility(View.GONE);
        findViewById(R.id.bPrepare).setVisibility(View.GONE);
        updateStatus("DISCONNECTED");
        findViewById(R.id.tvStatus).setVisibility(View.VISIBLE);

        sharedPreferences_editor.putBoolean("tizen_not_wear", tizen_not_wear);
        sharedPreferences_editor.apply();

        if(tizenNotWear){
            destroyWear();
            initTizen();
        }else{
            destroyTizen();
            initWear();
        }
    }
    private void initTizen(){
        if(!hasPackage(TIZEN_PACKAGE_NAME)){
            findViewById(R.id.tvStatus).setVisibility(View.GONE);
            findViewById(R.id.bConnect).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.tvError)).setText(R.string.noTizenLib);
            findViewById(R.id.tvError).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.samsung.accessory"))));
            return;
        }
        SAAgentV2.requestAgent(getApplicationContext(), CommsTizen.class.getName(), SAAgentCallback);
        findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
        updateStatus("DISCONNECTED");
    }
    private void destroyTizen(){
        if(comms_tizen != null){
            comms_tizen.closeConnection();
            comms_tizen.releaseAgent();
            comms_tizen = null;
        }
    }
    private void initWear(){
        if(!hasPackage(WEAR_PACKAGE_NAME)){
            findViewById(R.id.tvStatus).setVisibility(View.GONE);
            findViewById(R.id.bConnect).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.tvError)).setText(R.string.noWearLib);
            findViewById(R.id.tvError).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.wearable.app"))));
            return;
        }
        comms_wear = new CommsWear(getApplicationContext());
        findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
        findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
        findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
    }
    private void destroyWear(){
        if(comms_wear != null){
            comms_wear.stop();
            comms_wear = null;
        }
    }
    private void handleIntent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action.compareTo(Intent.ACTION_VIEW) != 0) return;

        String scheme = intent.getScheme();
        if(scheme.compareTo(ContentResolver.SCHEME_CONTENT) != 0){
            Log.e("MainActivity" , "Non supported scheme: " + scheme);
            return;
        }
        try{
            ContentResolver cr = getContentResolver();
            InputStream is = cr.openInputStream(intent.getData());
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder text = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null){
                text.append(line);
            }
            br.close();
            String matches_new_s = text.toString();
            JSONArray matches_new_ja = new JSONArray(matches_new_s);
            tabHistory.gotMatches(matches_new_ja);
        }catch(Exception e){
            Log.e("MainActivity", "handleIntent read file: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Problem with matches received from watch", Toast.LENGTH_SHORT).show();
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
    public void onConfigurationChanged(@NonNull Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        handleOrientation();
    }

    private void handleOrientation(){
        ImageView ivIcon = findViewById(R.id.ivIcon);
        Resources r = getResources();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics()));
        }else{
            ivIcon.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
            ivIcon.getLayoutParams().height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics()));
        }
    }
    @Override
    public void onBackPressed(){
        handler_main.removeCallbacks(this::explainDoubleBack);
        if(tabHistory.getVisibility() == View.VISIBLE){
            if(tabHistory.unselect()) return;
        }else if(tabReport.getVisibility() == View.VISIBLE){
            tabHistoryLabelClick();
            return;
        }else if(tabPrepare.getVisibility() == View.VISIBLE){
            tabReportLabelClick();
            return;
        }
        Date date = new Date();
        if(date.getTime() - back_press_time < 1000){
            finish();
            System.exit(0);
        }else{
            handler_main.postDelayed(this::explainDoubleBack, 1000);
        }
        back_press_time = date.getTime();
    }
    private void explainDoubleBack(){
        Toast.makeText(getApplicationContext(),"Press back twice to close", Toast.LENGTH_SHORT).show();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    @SuppressWarnings("unused")
    private boolean onTouchEventScrollViews(View v, MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener{
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
            try{
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if(Math.abs(diffX) > Math.abs(diffY)){
                    if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                        if(diffX > 0){
                            onSwipeRight();
                        }else{
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
            }catch(Exception e){
                Log.e("MainActivity", "onFling: " + e.getMessage());
            }
            return false;
        }
    }

    public void onSwipeRight(){
        if(tabReport.getVisibility() == View.VISIBLE){
            tabHistoryLabelClick();
        }else if(tabPrepare.getVisibility() == View.VISIBLE){
            tabReportLabelClick();
        }
    }

    public void onSwipeLeft(){
        if(tabHistory.getVisibility() == View.VISIBLE){
            tabReportLabelClick();
        }else if(tabReport.getVisibility() == View.VISIBLE){
            tabPrepareLabelClick();
        }
    }

    private final SAAgentV2.RequestAgentCallback SAAgentCallback = new SAAgentV2.RequestAgentCallback(){
        @Override
        public void onAgentAvailable(SAAgentV2 agent){
            if(!tizen_not_wear)return;
            comms_tizen = (CommsTizen) agent;
            findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(int errorCode, String errorMessage){
            Log.e("MainActivity", "Agent initialization error: " + errorCode + ". ErrorMsg: " + errorMessage);
            gotError(errorMessage);
        }
    };

    public void bConnectClick(){
        gotError("");
        findViewById(R.id.bConnect).setVisibility(View.GONE);
        if(comms_tizen == null){
            gotError("Watch not found");
            return;
        }
        comms_tizen.findPeers();
    }
    public void bSearchClick(){
        gotError("");
        ((TextView)findViewById(R.id.tvStatus)).setText(R.string.status_SEARCHING);
        findViewById(R.id.bSearch).setVisibility(View.GONE);
        comms_wear.search(getApplicationContext());
    }
    private void setButtonProcessing(int vid){
        findViewById(vid).setEnabled(false);
        handler_main.postDelayed(() -> findViewById(vid).setEnabled(true), 5000);
    }
    public void bGetMatchesClick(){
        setButtonProcessing(R.id.bGetMatches);
        if(cantSendRequest()){return;}
        gotError("");
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("deleted_matches", tabHistory.getDeletedMatches());
            requestData.put("custom_match_types", tabPrepare.getCustomMatchTypes());
            sendRequest("getMatches", requestData);
        }catch(Exception e){
            Log.e("MainActivity", "bGetMatchesClick exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Failed to request matches from watch", Toast.LENGTH_SHORT).show();
        }
    }
    public void bGetMatchClick(){
        setButtonProcessing(R.id.bGetMatch);
        if(cantSendRequest()){return;}
        gotError("");
        sendRequest("getMatch", null);
    }
    public void bPrepareClick(){
        setButtonProcessing(R.id.bPrepare);
        if(cantSendRequest()){return;}
        gotError("");
        JSONObject requestData = tabPrepare.getSettings();
        if(requestData == null){
            gotError("Error with settings");
            return;
        }
        sendRequest("prepare", requestData);
    }
    private void sendRequest(String requestType, JSONObject requestData){
        if(tizen_not_wear){
            comms_tizen.sendRequest(requestType, requestData);
        }else{
            CommsWear.sendRequest(this, requestType, requestData);
        }
    }
    private boolean cantSendRequest(){
        if(tizen_not_wear && comms_tizen != null &&
                comms_tizen.status.equals("CONNECTED")){
            return false;
        }
        if(!tizen_not_wear && comms_wear != null && (
                comms_wear.status.equals("CONNECTED") ||
                comms_wear.status.equals("OFFLINE")
        )){
            return false;
        }
        gotError(getString(R.string.first_connect));
        return true;
    }
    private void historyMatchClick(String match){
        try{
            JSONObject match_json = new JSONObject(match);
            tabReport.loadMatch(match_json);
            tabReportLabelClick();
        }catch(Exception e){
            gotError("Issue with match: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Failed to show match", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateStatus(final String status_new){
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvError = findViewById(R.id.tvError);
        tvStatus.setVisibility(View.VISIBLE);
        tvError.setText("");
        findViewById(R.id.bSearch).setVisibility(View.GONE);
        findViewById(R.id.bConnect).setVisibility(View.GONE);

        String status;
        switch(status_new){
            case "FATAL":
                findViewById(R.id.bConnect).setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                return;
            case "DISCONNECTED":
                status = getString(R.string.status_DISCONNECTED);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                break;
            case "FINDING_PEERS":
                status = getString(R.string.status_CONNECTING);
                break;
            case "CONNECTION_LOST":
                status = getString(R.string.status_CONNECTION_LOST);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                break;
            case "CONNECTED":
                status = getString(R.string.status_CONNECTED);
                findViewById(R.id.bGetMatches).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatch).setVisibility(View.VISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
                if(cantSendRequest()){break;}
                gotError("");
                try {
                    JSONObject requestData = new JSONObject();
                    requestData.put("deleted_matches", tabHistory.getDeletedMatches());
                    requestData.put("custom_match_types", tabPrepare.getCustomMatchTypes());
                    sendRequest("sync", requestData);
                }catch(Exception e){
                    Log.e("MainActivity", "updateStatus CONNECTED exception: " + e.getMessage());
                }
                break;
            case "OFFLINE":
                status = getString(R.string.status_OFFLINE);
                findViewById(R.id.bConnect).setVisibility(View.GONE);
                findViewById(R.id.bSearch).setVisibility(View.VISIBLE);
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
                tvStatus.setVisibility(View.GONE);
                status = getString(R.string.status_ERROR);
                findViewById(R.id.bConnect).setVisibility(View.VISIBLE);
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
        }
        tvStatus.setText(status);
    }

    public void gotResponse(final String requestType, final String responseData){
        int responseType = 0;
        if(responseData.startsWith("{")) responseType = 1;//JSONObject
        if(responseData.startsWith("[")) responseType = 2;//JSONArray
        try{
            switch(requestType){
                case "sync":
                    Log.i("MainActivity.gotResponse", "sync");
                    if(responseType != 1){break;}//Silently ignore for now
                    JSONObject syncResponse = new JSONObject(responseData);
                    if(!syncResponse.has("matches") || !syncResponse.has("settings")){
                        Log.e("MainActivity", "incomplete response");
                    }
                    tabHistory.gotMatches(syncResponse.getJSONArray("matches"));
                    tabPrepare.gotSettings(syncResponse.getJSONObject("settings"));
                    break;
                case "getMatches":
                    Log.i("MainActivity.gotResponse", "getMatches");
                    findViewById(R.id.bGetMatches).setEnabled(true);
                    if(responseType == 0){gotError(responseData);break;}
                    if(responseType != 2){gotError("invalid response");break;}
                    JSONArray getMatchesResponse = new JSONArray(responseData);
                    tabHistory.gotMatches(getMatchesResponse);
                    break;
                case "getMatch":
                    Log.i("MainActivity.gotResponse", "getMatch");
                    findViewById(R.id.bGetMatch).setEnabled(true);
                    if(responseType == 0){gotError(responseData);break;}
                    if(responseType != 1){gotError("invalid response");break;}
                    JSONObject getMatchResponse = new JSONObject(responseData);
                    tabReport.gotMatch(getMatchResponse);
                    break;
                case "prepare":
                    Log.i("MainActivity.gotResponse", "prepare");
                    findViewById(R.id.bPrepare).setEnabled(true);
                    if(!responseData.equals("okilly dokilly")){
                        gotError(responseData);
                    }
                    break;
            }
        }catch(Exception e){
            gotError("gotResponse exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Problem with message from watch", Toast.LENGTH_SHORT).show();
        }
    }
    public void gotError(String error){
        Log.i("MainActivity.gotError", error);
        if(error.equals(getString(R.string.did_not_understand_message))){
            error = getString(R.string.update_watch_app);
        }
        ((TextView)findViewById(R.id.tvError)).setText(error);
    }

    public void tabHistoryLabelClick(){
        hideKeyboard();
        findViewById(R.id.tabHistoryLabel).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabReportLabel).setBackgroundResource(0);
        findViewById(R.id.tabPrepareLabel).setBackgroundResource(0);
        tabHistory.setVisibility(View.VISIBLE);
        tabReport.setVisibility(View.GONE);
        tabPrepare.setVisibility(View.GONE);
    }
    public void tabReportLabelClick(){
        hideKeyboard();
        findViewById(R.id.tabHistoryLabel).setBackgroundResource(0);
        findViewById(R.id.tabReportLabel).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabPrepareLabel).setBackgroundResource(0);
        tabHistory.setVisibility(View.GONE);
        tabReport.setVisibility(View.VISIBLE);
        tabPrepare.setVisibility(View.GONE);
    }
    public void tabPrepareLabelClick(){
        hideKeyboard();
        findViewById(R.id.tabHistoryLabel).setBackgroundResource(0);
        findViewById(R.id.tabReportLabel).setBackgroundResource(0);
        findViewById(R.id.tabPrepareLabel).setBackgroundResource(R.drawable.tab_active);
        tabHistory.setVisibility(View.GONE);
        tabReport.setVisibility(View.GONE);
        tabPrepare.setVisibility(View.VISIBLE);
    }
    private void hideKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getApplicationWindowToken(),0);
    }

    public static String getTeamName(JSONObject team){
        String name = "";
        try{
            name = team.getString("team");
            if(team.has("id") && !team.getString("id").equals(name)){
                return name;
            }
            return name + " (" + team.getString("color") + ")";
        }catch(Exception e){
            Log.e("MainActivity", "getTeamName: " + e.getMessage());
        }
        return name;
    }

    private void exportMatches(){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "matches.json");

        exportMatchesActivityResultLauncher.launch(intent);
    }
    final ActivityResultLauncher<Intent> exportMatchesActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if(result.getResultCode() != Activity.RESULT_OK) return;
            Intent data = result.getData();
            if(data == null) return;
            Uri uri = data.getData();
            OutputStream outputStream;
            try{
                outputStream = getContentResolver().openOutputStream(uri);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
                bw.write(tabHistory.export_matches.toString());
                bw.flush();
                bw.close();
            }catch(Exception e){
                Log.e("MainActivity", "onActivityResult: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Failed to export matches", Toast.LENGTH_SHORT).show();
            }
        }
    );
}
