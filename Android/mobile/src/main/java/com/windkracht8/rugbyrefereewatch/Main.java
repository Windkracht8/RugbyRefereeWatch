package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends AppCompatActivity implements CommsBT.CommsBTInterface{
    static final String LOG_TAG = "RugbyRefereeWatch";
    private GestureDetector gestureDetector;
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor sharedPreferences_editor;
    static ExecutorService executorService;
    private Handler handler_main;
    static CommsBT commsBT;

    private ImageView icon;
    private TextView device;
    TabHistory tabHistory;
    TabReport tabReport;
    private TabPrepare tabPrepare;

    private boolean showSplash = true;
    private static boolean hasBTPermission = false;
    static int widthPixels;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> showSplash);
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
        icon.setColorFilter(getColor(R.color.icon_disabled));
        device = findViewById(R.id.device);
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

        checkPermissions();
        startBT();
        showSplash = false;
    }
    private void checkPermissions(){
        if(Build.VERSION.SDK_INT >= 31){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    && hasPermission(android.Manifest.permission.BLUETOOTH_SCAN);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this
                        ,new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT
                                ,Manifest.permission.BLUETOOTH_SCAN
                        }
                        ,1
                );
            }
        }else{
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this
                        ,new String[]{Manifest.permission.BLUETOOTH}
                        ,1
                );
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) ||
                permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
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
    private void startBT(){
        if(!hasBTPermission){
            onBTStartDone();
            return;
        }
        if(commsBT == null){
            commsBT = new CommsBT(this);
            commsBT.addListener(this);
        }
        executorService.submit(() -> commsBT.startComms());
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

    void toast(int message){
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
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
    boolean onTouchEventScrollViews(View ignoredV, MotionEvent event){
        return gestureDetector.onTouchEvent(event);
    }
    @Override
    public void onBTStartDone(){
        Log.d(LOG_TAG, "Main.onBTStartDone");
    }
    @Override
    public void onBTConnecting(String deviceName){
        Log.d(LOG_TAG, "Main.onBTConnecting");
        runOnUiThread(()-> {
            icon.setBackgroundResource(R.drawable.icon_watch_connecting);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            ((AnimatedVectorDrawable) icon.getBackground()).start();
            device.setTextColor(getColor(R.color.text));
            device.setText(rps(R.string.connecting_to, deviceName));
        });
    }
    @Override
    public void onBTConnectFailed(){
        Log.d(LOG_TAG, "Main.onBTConnectFailed");
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.error));
            device.setTextColor(getColor(R.color.error));
            device.setText(R.string.fail_BT);
        });
    }
    @Override
    public void onBTConnected(String deviceName){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.text));
            device.setTextColor(getColor(R.color.text));
            device.setText(rps(R.string.connected_to, deviceName));
            sendSyncRequest();
            findViewById(R.id.bSync).setVisibility(View.VISIBLE);
            findViewById(R.id.bPrepare).setVisibility(View.VISIBLE);
        });
    }
    @Override
    public void onBTDisconnected(){
        runOnUiThread(()->{
            icon.setColorFilter(getColor(R.color.icon_disabled));
            device.setTextColor(getColor(R.color.text));
            device.setText(R.string.disconnected);
            findViewById(R.id.bSync).setVisibility(View.GONE);
            findViewById(R.id.bPrepare).setVisibility(View.GONE);
        });
    }
    @Override
    public void onBTResponse(JSONObject response){
        try{
            String requestType = response.getString("requestType");
            switch(requestType){
                case "sync":
                    findViewById(R.id.bSync).setEnabled(true);
                    JSONObject syncResponseData = response.getJSONObject("responseData");
                    if(!syncResponseData.has("matches") || !syncResponseData.has("settings")){
                        Log.e(Main.LOG_TAG, "Main.onBTResponse sync: Incomplete response");
                    }
                    JSONArray matches = syncResponseData.getJSONArray("matches");
                    JSONObject settings = syncResponseData.getJSONObject("settings");
                    runOnUiThread(()->{
                        tabHistory.gotMatches(matches);
                        tabPrepare.gotSettings(settings);
                    });
                    break;
                case "prepare":
                    findViewById(R.id.bPrepare).setEnabled(true);
                    String prepareResponseData = response.getString("responseData");
                    switch(prepareResponseData){
                        case "unknown requestType"-> toast(R.string.update_watch_app);
                        case "match ongoing"-> toast(R.string.match_ongoing);
                        case "unexpected error"-> toast(R.string.fail_unexpected);
                    }
                    break;
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.onBTResponse: " + e.getMessage());
            toast(R.string.fail_response);
        }
    }
    @Override
    public void onBTError(int message){
        Log.d(LOG_TAG, "Main.onBTError");
        runOnUiThread(()->{
            icon.setColorFilter(getColor(R.color.error));
            device.setTextColor(getColor(R.color.error));
            device.setText(message);
        });
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
        handler_main.postDelayed(() -> findViewById(vid).setEnabled(true), 5000);
    }
    private void iconClick(){
        if(commsBT == null){
            startBT();
            return;
        }
        switch(commsBT.status){
            case CONNECTING:
            case CONNECTED:
                commsBT.disconnect();
                break;
            case DISCONNECTED:
                startActivity(new Intent(this, DeviceSelect.class));
                break;
        }
    }
    void bSyncClick(){
        setButtonProcessing(R.id.bSync);
        sendSyncRequest();
    }
    void bPrepareClick(){
        setButtonProcessing(R.id.bPrepare);
        if(cantSendRequest()){return;}
        JSONObject requestData = tabPrepare.getSettings();
        if(requestData == null){
            onBTError(R.string.fail_prepare);
            return;
        }
        commsBT.sendRequest("prepare", requestData);
    }
    private boolean cantSendRequest(){
        if(commsBT != null && commsBT.status == CommsBT.Status.CONNECTED) return false;
        device.setText(R.string.fail_BT);
        return true;
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

    void exportMatches(){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "matches.json");

        exportMatchesActivityResultLauncher.launch(intent);
    }
    private final ActivityResultLauncher<Intent> exportMatchesActivityResultLauncher = registerForActivityResult(
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
    private String rps(int resource, String string){return getString(resource) + " " + string;}
}
