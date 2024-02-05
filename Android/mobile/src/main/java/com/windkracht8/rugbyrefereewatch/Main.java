package com.windkracht8.rugbyrefereewatch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimatedVectorDrawable;
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
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends AppCompatActivity{
    public static final String LOG_TAG = "RugbyRefereeWatch";
    public static final int MESSAGE_TOAST = 101;
    public static final int MESSAGE_HISTORY_MATCH_CLICK = 102;
    public static final int MESSAGE_LOAD_LATEST_MATCH = 103;
    public static final int MESSAGE_DEL_CLICK = 104;
    public static final int MESSAGE_UPDATE_MATCH = 105;
    public static final int MESSAGE_EXPORT_MATCHES = 106;
    private GestureDetector gestureDetector;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor sharedPreferences_editor;
    private ExecutorService executorService;
    private Handler handler_main;
    private CommsBT commsBT;

    private TabHistory tabHistory;
    private TabReport tabReport;
    private TabPrepare tabPrepare;
    private ImageView icon;
    private ScrollView svBTLog;
    private LinearLayout llBTLog;

    public static int widthPixels = 0;
    private final ArrayList<String> prevStatuses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        setContentView(R.layout.main);
        getWidthPixels();
        handler_main = new Handler(Looper.getMainLooper());
        sharedPreferences = getSharedPreferences("com.windkracht8.rugbyrefereewatch", Context.MODE_PRIVATE);
        sharedPreferences_editor = sharedPreferences.edit();
        executorService = Executors.newFixedThreadPool(4);

        icon = findViewById(R.id.icon);
        icon.setOnClickListener(view -> iconClick());
        svBTLog = findViewById(R.id.svBTLog);
        llBTLog = findViewById(R.id.llBTLog);
        findViewById(R.id.tabHistoryLabel).setOnClickListener(view -> tabHistoryLabelClick());
        findViewById(R.id.tabReportLabel).setOnClickListener(view -> tabReportLabelClick());
        findViewById(R.id.tabPrepareLabel).setOnClickListener(view -> tabPrepareLabelClick());

        tabHistory = findViewById(R.id.tabHistory);
        tabHistory.onCreateMain(this);
        tabReport = findViewById(R.id.tabReport);
        tabReport.onCreateMain(this);
        tabPrepare = findViewById(R.id.tabPrepare);
        tabPrepare.onCreateMain(this);

        handleOrientation();
        handleIntent();

        try{
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version;
            if(android.os.Build.VERSION.SDK_INT >= 28){
                version = String.format("Version %s (%s)", packageInfo.versionName, packageInfo.getLongVersionCode());
            }else{
                version = String.format("Version %s (%s)", packageInfo.versionName, packageInfo.versionCode);
            }
            Log.d(Main.LOG_TAG, version);
            gotStatusUi(version);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBTLog getPackageInfo Exception: " + e.getMessage());
        }

        commsBT = new CommsBT(this);
        initBT();
    }
    public final Handler handler_message = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch(msg.what){
                case MESSAGE_HISTORY_MATCH_CLICK:
                    if(!(msg.obj instanceof JSONObject)) return;
                    tabReport.loadMatch(handler_message, (JSONObject) msg.obj);
                    tabReportLabelClick();
                    break;
                case MESSAGE_LOAD_LATEST_MATCH:
                    if(!(msg.obj instanceof JSONObject)) return;
                    tabReport.loadMatch(handler_message, (JSONObject) msg.obj);
                    break;
                case MESSAGE_DEL_CLICK:
                    tabReport.bDelClick(msg.arg1);
                    break;
                case MESSAGE_UPDATE_MATCH:
                    if(!(msg.obj instanceof JSONObject)) return;
                    tabHistory.updateMatch((JSONObject) msg.obj);
                    break;
                case MESSAGE_EXPORT_MATCHES:
                    exportMatches();
                    break;
                case MESSAGE_TOAST:
                    if(!(msg.obj instanceof Integer)) return;
                    Toast.makeText(getApplicationContext(), getString((Integer) msg.obj), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private void initBT(){
        if(Build.VERSION.SDK_INT >= 31){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1);
                return;
            }
        }else{
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 1);
                return;
            }
        }
        executorService.submit(() -> commsBT.startComms());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    initBT();
                }else{
                    updateStatus(CommsBT.Status.FATAL);
                    gotError(getString(R.string.fail_BT_denied));
                }
                return;
            }
        }
    }
    private void handleIntent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action == null || !action.equals(Intent.ACTION_VIEW)) return;

        String scheme = intent.getScheme();
        if(scheme == null || !scheme.equals(ContentResolver.SCHEME_CONTENT)){
            Log.e(Main.LOG_TAG, "Main.handleIntent Non supported scheme: " + scheme);
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
            Log.e(Main.LOG_TAG, "Main.handleIntent Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.problem_matches_received, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        handleOrientation();
    }

    private void handleOrientation(){
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
        finish();
        System.exit(0);
    }
    private void explainDoubleBack(){
        Toast.makeText(getApplicationContext(), R.string.explainDoubleBack, Toast.LENGTH_SHORT).show();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    public boolean onTouchEventScrollViews(View ignoredV, MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener{
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY){
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
                Log.e(Main.LOG_TAG, "Main.onFling Exception: " + e.getMessage());
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
    public void iconClick(){
        if(commsBT == null || commsBT.status == CommsBT.Status.SEARCH_TIMEOUT){
            initBT();
        }
        if(commsBT != null && commsBT.status == CommsBT.Status.SEARCHING){
            commsBT.updateStatus(CommsBT.Status.SEARCH_TIMEOUT);
            commsBT.stopComms();
        }
    }
    public void bSyncClick(){
        setButtonProcessing(R.id.bSync);
        sendSyncRequest();
    }
    public void bPrepareClick(){
        setButtonProcessing(R.id.bPrepare);
        if(cantSendRequest()){return;}
        JSONObject requestData = tabPrepare.getSettings();
        if(requestData == null){
            gotError(getString(R.string.fail_prepare));
            return;
        }
        commsBT.sendRequest("prepare", requestData);
    }
    private boolean cantSendRequest(){
        if(commsBT != null && commsBT.status == CommsBT.Status.CONNECTED){
            return false;
        }
        gotError(getString(R.string.first_connect));
        return true;
    }

    public void updateStatus(final CommsBT.Status status){
        runOnUiThread(() -> updateStatusUi(status));
    }
    private void updateStatusUi(final CommsBT.Status status){
        switch(status){
            case FATAL:
                icon.setBackgroundResource(R.drawable.icon_watch);
                icon.setColorFilter(getColor(R.color.error), android.graphics.PorterDuff.Mode.SRC_IN);
                findViewById(R.id.bSync).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                return;
            case SEARCHING:
                icon.setBackgroundResource(R.drawable.icon_watch_searching);
                icon.setColorFilter(getColor(R.color.icon_disabled), android.graphics.PorterDuff.Mode.SRC_IN);
                ((AnimatedVectorDrawable) icon.getBackground()).start();
                prevStatuses.clear();
                gotStatus(getString(R.string.status_SEARCHING));
                findViewById(R.id.bSync).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                break;
            case SEARCH_TIMEOUT:
                icon.setBackgroundResource(R.drawable.icon_watch);
                icon.setColorFilter(getColor(R.color.icon_disabled), android.graphics.PorterDuff.Mode.SRC_IN);
                gotError(getString(R.string.status_SEARCH_TIMEOUT));
                findViewById(R.id.bSync).setVisibility(View.GONE);
                findViewById(R.id.bPrepare).setVisibility(View.GONE);
                break;
            case CONNECTED:
                icon.setBackgroundResource(R.drawable.icon_watch);
                icon.setColorFilter(getColor(R.color.text), android.graphics.PorterDuff.Mode.SRC_IN);
                gotStatus(getString(R.string.status_CONNECTED));
                findViewById(R.id.bSync).setVisibility(View.VISIBLE);
                findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
                sendSyncRequest();
                break;
        }
    }
    private void sendSyncRequest(){
        if(cantSendRequest()){return;}
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("deleted_matches", tabHistory.getDeletedMatches());
            requestData.put("custom_match_types", tabPrepare.getCustomMatchTypes());
            commsBT.sendRequest("sync", requestData);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.sendSyncRequest Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_sync, Toast.LENGTH_SHORT).show();
        }
    }

    public void gotResponse(final JSONObject response){
        runOnUiThread(() -> gotResponseUi(response));
    }
    private void gotResponseUi(final JSONObject response){
        try{
            String requestType = response.getString("requestType");
            gotStatus(String.format("%s %s", getString(R.string.received_response), requestType));
            switch(requestType){
                case "sync":
                    findViewById(R.id.bSync).setEnabled(true);
                    JSONObject syncResponseData = response.getJSONObject("responseData");
                    if(!syncResponseData.has("matches") || !syncResponseData.has("settings")){
                        Log.e(Main.LOG_TAG, "Main.gotResponse sync: Incomplete response");
                    }
                    tabHistory.gotMatches(syncResponseData.getJSONArray("matches"));
                    tabPrepare.gotSettings(syncResponseData.getJSONObject("settings"));
                    break;
                case "prepare":
                    findViewById(R.id.bPrepare).setEnabled(true);
                    String prepareResponseData = response.getString("responseData");
                    if(!prepareResponseData.equals("okilly dokilly")){
                        gotError(prepareResponseData);
                    }
                    break;
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.gotResponse: " + e.getMessage());
            Toast.makeText(this, R.string.fail_response, Toast.LENGTH_SHORT).show();
        }
    }
    public void gotStatus(String status){
        runOnUiThread(() -> gotStatusUi(status));
    }
    private void gotStatusUi(String status){
        if(prevStatuses.contains(status)){
            return;
        }
        prevStatuses.add(status);
        if(prevStatuses.size()>2) prevStatuses.remove(0);

        TextView tv = new TextView(this);
        tv.setText(status);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        llBTLog.addView(tv);
        tv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                tv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                svBTLog.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    public void gotError(String error){
        Log.d(Main.LOG_TAG, "Main.gotError: " + error);
        switch(error){
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
        final String error2 = error;
        runOnUiThread(() -> gotErrorUi(error2));
    }
    private void gotErrorUi(String error){
        TextView tv = new TextView(this);
        tv.setText(error);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(getColor(R.color.error));
        llBTLog.addView(tv);
        tv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                tv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                svBTLog.fullScroll(View.FOCUS_DOWN);
            }
        });
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
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
            Log.e(Main.LOG_TAG, "Main.getTeamName Exception: " + e.getMessage());
        }
        return name;
    }

    public void exportMatches(){
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
                Log.e(Main.LOG_TAG, "Main.onActivityResult Exception: " + e.getMessage());
                Toast.makeText(getApplicationContext(), R.string.fail_export, Toast.LENGTH_SHORT).show();
            }
        }
    );
    private void getWidthPixels(){
        if(Build.VERSION.SDK_INT >= 30){
            widthPixels = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            widthPixels = displayMetrics.widthPixels;
        }
    }
}
