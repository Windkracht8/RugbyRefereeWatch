package com.windkracht8.rugbyrefereewatch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Objects;

public class Main extends AppCompatActivity{
    public static final String RRW_LOG_TAG = "RugbyRefereeWatch";
    public static final int MESSAGE_GOT_ERROR = 1;
    public static final int MESSAGE_GOT_RESPONSE = 2;
    public static final int MESSAGE_UPDATE_STATUS = 3;
    public static final int MESSAGE_HISTORY_MATCH_CLICK = 4;
    public static final int MESSAGE_DEL_CLICK = 5;
    public static final int MESSAGE_UPDATE_MATCH = 6;
    public static final int MESSAGE_EXPORT_MATCHES = 7;
    private GestureDetector gestureDetector;
    private Comms comms = null;
    public static SharedPreferences.Editor sharedPreferences_editor;

    private long back_press_time;
    private Handler handler_main;
    public static int widthPixels = 0;

    private TabHistory tabHistory;
    private TabReport tabReport;
    private TabPrepare tabPrepare;

    @SuppressLint({"MissingInflatedId"}) //bGetMatches, bGetMatch, bPrepare are in separate layouts
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        setContentView(R.layout.main);
        getWidthPixels();
        tabHistory = findViewById(R.id.tabHistory);
        tabHistory.loadMatches(handler_message);
        tabReport = findViewById(R.id.tabReport);
        tabPrepare = findViewById(R.id.tabPrepare);
        findViewById(R.id.tabHistoryLabel).setOnClickListener(view -> tabHistoryLabelClick());
        findViewById(R.id.tabReportLabel).setOnClickListener(view -> tabReportLabelClick());
        findViewById(R.id.tabPrepareLabel).setOnClickListener(view -> tabPrepareLabelClick());
        findViewById(R.id.bGetMatches).setOnClickListener(view -> bGetMatchesClick());
        findViewById(R.id.bGetMatch).setOnClickListener(view -> bGetMatchClick());
        findViewById(R.id.bExport).setOnClickListener(view -> exportMatches());
        findViewById(R.id.bPrepare).setOnClickListener(view -> bPrepareClick());
        handleOrientation();

        SharedPreferences sharedPreferences = getSharedPreferences("com.windkracht8.rugbyrefereewatch", Context.MODE_PRIVATE);
        sharedPreferences_editor = sharedPreferences.edit();

        TabPrepare.sHomeColorPosition = sharedPreferences.getInt("sHomeColorPosition", 0);
        ((Spinner)findViewById(R.id.sHomeColor)).setSelection(TabPrepare.sHomeColorPosition);
        TabPrepare.sAwayColorPosition = sharedPreferences.getInt("sAwayColorPosition", 0);
        ((Spinner)findViewById(R.id.sAwayColor)).setSelection(TabPrepare.sAwayColorPosition);

        TabPrepare.sHomeColorPosition = sharedPreferences.getInt("sHomeColorPosition", 0);
        ((Spinner)findViewById(R.id.sHomeColor)).setSelection(TabPrepare.sHomeColorPosition);
        TabPrepare.sAwayColorPosition = sharedPreferences.getInt("sAwayColorPosition", 0);
        ((Spinner)findViewById(R.id.sAwayColor)).setSelection(TabPrepare.sAwayColorPosition);

        handleIntent();
        handler_main = new Handler(Looper.getMainLooper());

        findViewById(R.id.scrollHistory).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.llMatches).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.scrollReport).setOnTouchListener(this::onTouchEventScrollViews);
        findViewById(R.id.scrollPrepare).setOnTouchListener(this::onTouchEventScrollViews);

        initBT();
    }
    public final Handler handler_message = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch(msg.what){
                case MESSAGE_GOT_ERROR: //"gotError":
                    if(!(msg.obj instanceof String)) return;
                    gotError((String) msg.obj);
                    break;
                case MESSAGE_GOT_RESPONSE:
                    if(!(msg.obj instanceof JSONObject)) return;
                    gotResponse((JSONObject) msg.obj);
                    break;
                case MESSAGE_UPDATE_STATUS: //"updateStatus":
                    if(!(msg.obj instanceof String)) return;
                    updateStatus((String) msg.obj);
                    break;
                case MESSAGE_HISTORY_MATCH_CLICK:
                    if(!(msg.obj instanceof JSONObject)) return;
                    historyMatchClick((JSONObject) msg.obj);
                    break;
                case MESSAGE_DEL_CLICK:
                    tabReport.bDelClick(msg.arg1);
                    break;
                case MESSAGE_UPDATE_MATCH: //"updateMatch":
                    if(!(msg.obj instanceof JSONObject)) return;
                    tabHistory.updateMatch((JSONObject) msg.obj);
                    break;
                case MESSAGE_EXPORT_MATCHES: //"exportMatches":
                    exportMatches();
                    break;
            }
        }
    };
    private void initBT(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
            updateStatus("FATAL");
            gotError(getString(R.string.fail_BT_denied));
            return;
        }
        if(comms == null) comms = new Comms(this, handler_message);
        comms.startListening();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initBT();
        }else{
            updateStatus("FATAL");
            gotError(getString(R.string.fail_BT_denied));
        }
    }
    private void handleIntent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action == null || action.compareTo(Intent.ACTION_VIEW) != 0) return;

        String scheme = intent.getScheme();
        if(scheme == null || scheme.compareTo(ContentResolver.SCHEME_CONTENT) != 0){
            Log.e(Main.RRW_LOG_TAG, "Main.handleIntent Non supported scheme: " + scheme);
            return;
        }
        try{
            ContentResolver cr = getContentResolver();

            InputStream is = cr.openInputStream(Objects.requireNonNull(intent.getData()));
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
            Log.e(Main.RRW_LOG_TAG, "Main.handleIntent Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.problem_matches_received, Toast.LENGTH_SHORT).show();
        }
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
        getWidthPixels();
        TabReport.what_width = 0;
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
        Toast.makeText(getApplicationContext(), R.string.explainDoubleBack, Toast.LENGTH_SHORT).show();
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
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY){
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
                Log.e(Main.RRW_LOG_TAG, "Main.onFling Exception: " + e.getMessage());
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
            comms.sendRequest("getMatches", requestData);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.bGetMatchesClick Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_get_matches, Toast.LENGTH_SHORT).show();
        }
    }
    public void bGetMatchClick(){
        setButtonProcessing(R.id.bGetMatch);
        if(cantSendRequest()){return;}
        gotError("");
        comms.sendRequest("getMatch", null);
    }
    public void bPrepareClick(){
        setButtonProcessing(R.id.bPrepare);
        if(cantSendRequest()){return;}
        gotError("");
        JSONObject requestData = tabPrepare.getSettings();
        if(requestData == null){
            gotError(getString(R.string.fail_prepare));
            return;
        }
        comms.sendRequest("prepare", requestData);
    }
    private boolean cantSendRequest(){
        if(comms != null && comms.status.equals("CONNECTED")){
            return false;
        }
        gotError(getString(R.string.first_connect));
        return true;
    }
    private void historyMatchClick(JSONObject match){
        try{
            tabReport.loadMatch(handler_message, match);
            tabReportLabelClick();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.historyMatchClick Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateStatus(final String status_new){
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvError = findViewById(R.id.tvError);
        tvStatus.setVisibility(View.VISIBLE);
        tvError.setText("");

        String status;
        switch(status_new){
            case "FATAL"://TODO: find out what statuses still happen
                tvStatus.setVisibility(View.GONE);
                return;
            case "DISCONNECTED":
                status = getString(R.string.status_DISCONNECTED);
                break;
            case "LISTENING":
                status = getString(R.string.status_LISTENING);
                break;
            case "CONNECTION_LOST":
                status = getString(R.string.status_CONNECTION_LOST);
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
                    comms.sendRequest("sync", requestData);
                }catch(Exception e){
                    Log.e(Main.RRW_LOG_TAG, "Main.updateStatus CONNECTED Exception: " + e.getMessage());
                }
                break;
            case "OFFLINE":
                status = getString(R.string.status_OFFLINE);
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
                findViewById(R.id.bGetMatches).setVisibility(View.GONE);
                findViewById(R.id.bGetMatch).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
        }
        tvStatus.setText(status);
    }

    public void gotResponse(final JSONObject response){
        try{
            String requestType = response.getString("requestType");
            switch(requestType){
                case "sync":
                    Log.i(Main.RRW_LOG_TAG, "Main.gotResponse sync");
                    JSONObject syncResponseData = response.getJSONObject("responseData");
                    if(!syncResponseData.has("matches") || !syncResponseData.has("settings")){
                        Log.e(Main.RRW_LOG_TAG, "Main.gotResponse sync: Incomplete response");
                    }
                    tabHistory.gotMatches(syncResponseData.getJSONArray("matches"));
                    tabPrepare.gotSettings(syncResponseData.getJSONObject("settings"));
                    break;
                case "getMatches":
                    Log.i(Main.RRW_LOG_TAG, "Main.gotResponse getMatches");
                    findViewById(R.id.bGetMatches).setEnabled(true);
                    JSONArray matchesResponseData = response.getJSONArray("responseData");
                    tabHistory.gotMatches(matchesResponseData);
                    break;
                case "getMatch":
                    Log.i(Main.RRW_LOG_TAG, "Main.gotResponse getMatch");
                    findViewById(R.id.bGetMatch).setEnabled(true);
                    JSONObject matchResponseData = response.getJSONObject("responseData");
                    tabReport.gotMatch(handler_message, matchResponseData);
                    break;
                case "prepare":
                    Log.i(Main.RRW_LOG_TAG, "Main.gotResponse prepare");
                    findViewById(R.id.bPrepare).setEnabled(true);
                    String prepareResponseData = response.getString("responseData");
                    if(!prepareResponseData.equals("okilly dokilly")){
                        gotError(prepareResponseData);
                    }
                    break;
            }
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.gotResponse: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_response, Toast.LENGTH_SHORT).show();
        }
    }
    public void gotError(String error){
        Log.i(Main.RRW_LOG_TAG, "Main.gotError: " + error);
        switch(error){
            case "Did not understand message"://DEPRECATED
            case "unknown requestType":
                error = getString(R.string.update_watch_app);
                break;
            case "match ongoing":
                error = getString(R.string.match_ongoing);
                break;
            case "unexpected error":
                error = getString(R.string.fail_unexpected);
                break;
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

    public static String getTeamName(Context context, JSONObject team){
        String name = "";
        try{
            name = team.getString("team");
            if(team.has("id") && !team.getString("id").equals(name)){
                return name;
            }
            return name + " (" + Translator.getTeamColorLocal(context, team.getString("color")) + ")";
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.getTeamName Exception: " + e.getMessage());
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
                assert uri != null;
                outputStream = getContentResolver().openOutputStream(uri);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
                bw.write(tabHistory.getSelectedMatches());
                bw.flush();
                bw.close();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "Main.onActivityResult Exception: " + e.getMessage());
                Toast.makeText(getApplicationContext(), R.string.fail_export, Toast.LENGTH_SHORT).show();
            }
        }
    );
    private void getWidthPixels(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            widthPixels = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            widthPixels = displayMetrics.widthPixels;
        }
    }
}
