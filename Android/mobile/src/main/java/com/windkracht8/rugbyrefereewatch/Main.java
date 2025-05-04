package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends AppCompatActivity implements CommsBT.BTInterface{
    static final String LOG_TAG = "RugbyRefereeWatch";
    static final String HOME_ID = "home";
    static final String AWAY_ID = "away";
    private GestureDetector gestureDetector;
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor sharedPreferences_editor;
    private ExecutorService executorService;
    private Handler handler;
    static CommsBT commsBT;

    private ImageView icon;
    private TextView device;
    TabHistory tabHistory;
    TabReport tabReport;
    private TabPrepare tabPrepare;

    private boolean showSplash = true;
    private static boolean hasBTPermission = false;

    @Override protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(()->showSplash);
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());
        setContentView(R.layout.main);
        findViewById(android.R.id.content).setOnApplyWindowInsetsListener(onApplyWindowInsetsListener);

        sharedPreferences = getSharedPreferences("main", Context.MODE_PRIVATE);
        sharedPreferences_editor = sharedPreferences.edit();
        handler = new Handler(Looper.getMainLooper());

        icon = findViewById(R.id.icon);
        icon.setOnClickListener(v->iconClick());
        icon.setColorFilter(getColor(R.color.icon_disabled));
        device = findViewById(R.id.device);
        findViewById(R.id.tabHistoryLabel).setOnClickListener(v->tabHistoryLabelClick());
        findViewById(R.id.tabReportLabel).setOnClickListener(v->tabReportLabelClick());
        findViewById(R.id.tabPrepareLabel).setOnClickListener(v->tabPrepareLabelClick());

        tabHistory = findViewById(R.id.tabHistory);
        tabHistory.onCreateMain(this);
        tabReport = findViewById(R.id.tabReport);
        tabReport.onCreateMain(this);
        tabPrepare = findViewById(R.id.tabPrepare);
        tabPrepare.onCreateMain(this);

        checkPermissions();
        startBT();
        showSplash = false;
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        runInBackground(()->{
            if(commsBT != null) commsBT.stopBT();
            commsBT = null;
        });
    }
    @Override protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT < 35) return;
        WindowInsetsController wic = icon.getWindowInsetsController();
        if(wic == null) return;
        switch(getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK){
            case Configuration.UI_MODE_NIGHT_NO:
                wic.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                wic.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
                break;
        }
    }

    private void checkPermissions(){
        if(Build.VERSION.SDK_INT >= 31){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    && hasPermission(android.Manifest.permission.BLUETOOTH_SCAN);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        1
                );
            }
        }else{
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        1
                );
            }
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    hasBTPermission = true;
                    startBT();
                }else{
                    onBTError(R.string.fail_BT_denied);
                }
                return;
            }
        }
    }
    private boolean hasPermission(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }

    private void startBT(){
        if(!hasBTPermission){
            onBTStartDone();
            return;
        }
        commsBT = new CommsBT(this);
        commsBT.addListener(this);
        runInBackground(commsBT::startBT);
    }

    static final View.OnApplyWindowInsetsListener onApplyWindowInsetsListener = new View.OnApplyWindowInsetsListener(){
        @NonNull @Override public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets windowInsets){
            if(Build.VERSION.SDK_INT >= 30){
                Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }else{
                view.setPadding(
                        windowInsets.getSystemWindowInsetLeft(),
                        windowInsets.getSystemWindowInsetTop(),
                        windowInsets.getSystemWindowInsetRight(),
                        windowInsets.getSystemWindowInsetBottom()
                );
            }
            return windowInsets;
        }
    };

    void toast(int message){
        runOnUiThread(()->Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            onBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

     private void onBack(){
        if(tabHistory.getVisibility() == View.VISIBLE && tabHistory.unselect()){
            return;
        }else if(tabReport.getVisibility() == View.VISIBLE){
            tabHistoryLabelClick();
            return;
        }
        finish();
    }
    @Override public boolean onTouchEvent(MotionEvent event){return gestureDetector.onTouchEvent(event);}
    boolean onTouchEventScrollViews(View ignoredV, MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    @Override public void onBTStartDone(){
        if(commsBT == null){
            onBTError(R.string.fail_BT_denied);
            return;
        }
        if(commsBT.status == CommsBT.Status.CONNECTED) return;
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            device.setTextColor(getColor(R.color.text));
            device.setText(R.string.connect);
        });
    }
    @Override public void onBTConnecting(String deviceName){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch_connecting);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            ((AnimatedVectorDrawable) icon.getBackground()).start();
            device.setTextColor(getColor(R.color.text));
            device.setText(getString(R.string.connecting_to, deviceName));
        });
    }
    @Override public void onBTConnectFailed(){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.error));
            device.setTextColor(getColor(R.color.error));
            device.setText(R.string.fail_BT);
        });
    }
    @Override public void onBTConnected(String deviceName){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.text));
            device.setTextColor(getColor(R.color.text));
            device.setText(getString(R.string.connected_to, deviceName));
            sendSyncRequest();
            findViewById(R.id.bSync).setVisibility(View.VISIBLE);
            findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
        });
    }
    @Override public void onBTDisconnected(){
        runOnUiThread(()->{
            icon.setColorFilter(getColor(R.color.icon_disabled));
            device.setTextColor(getColor(R.color.text));
            device.setText(R.string.disconnected);
            findViewById(R.id.bSync).setVisibility(View.GONE);
            findViewById(R.id.bPrepare).setVisibility(View.GONE);
        });
    }
    @Override public void onBTResponse(JSONObject response){
        try{
            String requestType = response.getString("requestType");
            switch(requestType){
                case "sync":
                    JSONObject syncResponseData = response.getJSONObject("responseData");
                    if(!syncResponseData.has("matches") || !syncResponseData.has("settings")){
                        Log.e(Main.LOG_TAG, "Main.onBTResponse sync: Incomplete response");
                    }
                    JSONArray matches = syncResponseData.getJSONArray("matches");
                    JSONObject settings = syncResponseData.getJSONObject("settings");
                    tabHistory.gotMatches(matches);
                    runOnUiThread(()->{
                        findViewById(R.id.bSync).setEnabled(true);
                        tabPrepare.gotSettings(settings);
                    });
                    break;
                case "prepare":
                    runOnUiThread(()->findViewById(R.id.bPrepare).setEnabled(true));
                    String prepareResponseData = response.getString("responseData");
                    switch(prepareResponseData){
                        case "unknown requestType"-> onBTError(R.string.update_watch_app);
                        case "match ongoing"-> onBTError(R.string.match_ongoing);
                        case "unexpected error"-> onBTError(R.string.fail_unexpected);
                    }
                    break;
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.onBTResponse: " + e.getMessage());
            onBTError(R.string.fail_response);
        }
    }
    @Override public void onBTError(int message){
        Log.d(LOG_TAG, "Main.onBTError: " + getString(message));
        runOnUiThread(()->{
            icon.setColorFilter(getColor(R.color.error));
            device.setTextColor(getColor(R.color.error));
            device.setText(message);
        });
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener{
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY){
            try{
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if(Math.abs(diffX) > Math.abs(diffY)){
                    if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                        HistoryMatch.isLongPress = false;
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

    private void onSwipeRight(){
        if(tabReport.getVisibility() == View.VISIBLE){
            tabHistoryLabelClick();
        }else if(tabPrepare.getVisibility() == View.VISIBLE){
            tabReportLabelClick();
        }
    }

    private void onSwipeLeft(){
        if(tabHistory.getVisibility() == View.VISIBLE){
            tabReportLabelClick();
        }else if(tabReport.getVisibility() == View.VISIBLE){
            tabPrepareLabelClick();
        }
    }

    private void setButtonProcessing(int vid){
        findViewById(vid).setEnabled(false);
        handler.postDelayed(()->findViewById(vid).setEnabled(true), 5000);
    }
    private void iconClick(){
        if(commsBT == null) return;
        switch(commsBT.status){
            case CONNECTING, CONNECTED -> commsBT.stopBT();
            case DISCONNECTED ->{
                Intent startDeviceSelect = new Intent(this, DeviceSelect.class);
                startDeviceSelect.putExtra("restartBT", true);
                startActivity(startDeviceSelect);
            }
        }
    }
    void bSyncClick(){
        setButtonProcessing(R.id.bSync);
        if(cantSendRequest()){
            device.setText(R.string.fail_BT);
            return;
        }
        sendSyncRequest();
    }
    void bPrepareClick(){
        setButtonProcessing(R.id.bPrepare);
        if(cantSendRequest()){
            device.setText(R.string.fail_BT);
            return;
        }
        JSONObject requestData = tabPrepare.getSettings();
        if(requestData == null){
            onBTError(R.string.fail_prepare);
            return;
        }
        commsBT.sendRequest("prepare", requestData);
    }
    private boolean cantSendRequest(){
        return commsBT == null || commsBT.status != CommsBT.Status.CONNECTED;
    }

    void sendSyncRequest(){
        if(cantSendRequest()) return;
        try{
            JSONObject requestData = new JSONObject();
            requestData.put("deleted_matches", tabHistory.getDeletedMatches());
            requestData.put("custom_match_types", tabPrepare.getCustomMatchTypes());
            commsBT.sendRequest("sync", requestData);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.sendSyncRequest Exception: " + e.getMessage());
            onBTError(R.string.fail_sync);
        }
    }

    private void tabHistoryLabelClick(){
        hideKeyboard();
        findViewById(R.id.tabHistoryLabel).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabReportLabel).setBackgroundResource(0);
        findViewById(R.id.tabPrepareLabel).setBackgroundResource(0);
        tabHistory.setVisibility(View.VISIBLE);
        tabReport.setVisibility(View.GONE);
        tabPrepare.setVisibility(View.GONE);
    }
    void tabReportLabelClick(){
        hideKeyboard();
        findViewById(R.id.tabHistoryLabel).setBackgroundResource(0);
        findViewById(R.id.tabReportLabel).setBackgroundResource(R.drawable.tab_active);
        findViewById(R.id.tabPrepareLabel).setBackgroundResource(0);
        tabHistory.setVisibility(View.GONE);
        tabReport.setVisibility(View.VISIBLE);
        tabPrepare.setVisibility(View.GONE);
    }
    private void tabPrepareLabelClick(){
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

    String getTeamName(JSONObject team){
        return getTeamName(this, team);
    }
    static String getTeamName(Context context, JSONObject team){
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

    void importMatches(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importMatchesResult.launch(intent);
    }
    private final ActivityResultLauncher<Intent> importMatchesResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result->{
            if(result.getResultCode() != Activity.RESULT_OK) return;
            try{
                Intent data = result.getData();
                if(data == null) throw new Exception("data is empty");
                Uri uri = data.getData();
                if(uri == null) throw new Exception("uri is empty");
                InputStream is = getContentResolver().openInputStream(uri);
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
                Log.e(Main.LOG_TAG, "Main.importMatchesResult Exception: " + e.getMessage());
                onBTError(R.string.fail_import);
            }
        }
    );

    void exportMatches(){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "matches.json");
        exportMatchesResult.launch(intent);
    }
    private final ActivityResultLauncher<Intent> exportMatchesResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result->{
            if(result.getResultCode() != Activity.RESULT_OK) return;
            try{
                Intent data = result.getData();
                if(data == null) throw new Exception("data is empty");
                Uri uri = data.getData();
                if(uri == null) throw new Exception("uri is empty");
                OutputStream os = getContentResolver().openOutputStream(uri);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write(tabHistory.getSelectedMatches());
                bw.flush();
                bw.close();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "Main.exportMatchesResult Exception: " + e.getMessage());
                onBTError(R.string.fail_export);
            }
        }
    );
}
