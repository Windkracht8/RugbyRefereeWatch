/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class Main extends AppCompatActivity implements Comms.Interface{
    static final String LOG_TAG = "RugbyRefereeWatch";
    static final String HOME_ID = "home";
    static final String AWAY_ID = "away";
    private GestureDetector gestureDetector;
    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor sharedPreferences_editor;
    private ExecutorService executorService;
    private Handler handler;
    static Comms comms;

    private ImageView icon;
    private TextView device;
    TabHistory tabHistory;
    TabReport tabReport;
    private TabPrepare tabPrepare;
    private Button bSync;
    private Button bPrepare;

    private boolean showSplash = true;

    @SuppressLint("MissingInflatedId")//bSync and bPrepare
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

        bSync = findViewById(R.id.bSync);
        bPrepare = findViewById(R.id.bPrepare);

        showSplash = false;
        Permissions.checkPermissions(this);
        if(comms == null || comms.status == Comms.Status.DISCONNECTED) startComms();
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        runInBackground(()->{
            if(comms != null){
                comms.stop();
                comms.onDestroy(this);
            }
            comms = null;
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

    void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }

    private void startComms(){
        if(!Permissions.hasBTPermission){
            onCommsStartDone();
            return;
        }
        icon.setBackgroundResource(R.drawable.icon_watch_connecting);
        icon.setColorFilter(getColor(R.color.icon_disabled));
        ((AnimatedVectorDrawable) icon.getBackground()).start();
        comms = new Comms(this);
        runInBackground(comms::start);
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
    @Override public void onCommsStartDone(){
        if(comms == null ||
                comms.status == Comms.Status.CONNECTED_BT ||
                comms.status == Comms.Status.CONNECTED_IQ
        ) return;
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            device.setTextColor(getColor(R.color.text));
            device.setText(R.string.connect);
        });
    }
    @Override public void onCommsConnecting(String deviceName){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch_connecting);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            ((AnimatedVectorDrawable) icon.getBackground()).start();
            device.setTextColor(getColor(R.color.text));
            device.setText(getString(R.string.connecting_to, deviceName));
        });
    }
    @Override public void onCommsConnectFailed(){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.error));
            device.setTextColor(getColor(R.color.error));
            device.setText(R.string.fail_BT);
        });
    }
    @Override public void onCommsConnected(String deviceName){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.text));
            device.setTextColor(getColor(R.color.text));
            device.setText(getString(R.string.connected_to, deviceName));
            bSync.setVisibility(View.VISIBLE);
            bPrepare.setVisibility(View.VISIBLE);
        });
    }
    @Override public void onCommsSending(){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch_connecting);
            ((AnimatedVectorDrawable) icon.getBackground()).start();
            bSync.setEnabled(false);
            bSync.setTextColor(getColor(R.color.icon_disabled));
            bPrepare.setEnabled(false);
            bPrepare.setTextColor(getColor(R.color.icon_disabled));
        });
    }
    @Override public void onCommsSendingFinished(){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            bSync.setEnabled(true);
            bSync.setTextColor(getColor(R.color.button));
            bPrepare.setEnabled(true);
            bPrepare.setTextColor(getColor(R.color.button));
        });
    }
    @Override public void onCommsDisconnected(){
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
            icon.setColorFilter(getColor(R.color.icon_disabled));
            device.setTextColor(getColor(R.color.text));
            device.setText(R.string.disconnected);
            bSync.setVisibility(View.GONE);
            bPrepare.setVisibility(View.GONE);
        });
    }
    @Override public void onCommsResponse(Comms.Request.Type requestType, JSONObject responseData){
        try{
            switch(requestType){
                case SYNC:
                    if(responseData.has("matches")){//DEPRECATED
                        JSONArray matches = responseData.getJSONArray("matches");
                        tabHistory.gotMatches(matches);
                    }
                    if(responseData.has("settings")){
                        JSONObject settings = responseData.getJSONObject("settings");
                        runOnUiThread(()->tabPrepare.gotSettings(settings));
                    }
                    break;
                case GET_MATCH:
                    tabHistory.insertMatch(responseData);
                    runOnUiThread(()->tabHistory.showMatches(true));
                    break;
                case PREP:
                    runOnUiThread(()->bPrepare.setEnabled(true));
                    break;
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.onCommsResponse: " + e.getMessage());
            onCommsError(R.string.fail_response);
        }
    }
    @Override public void onCommsError(int message){
        Log.d(LOG_TAG, "Main.onCommsError: " + getString(message));
        runOnUiThread(()->{
            icon.setBackgroundResource(R.drawable.icon_watch);
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
        if(!Permissions.hasBTPermission){
            startActivity(new Intent(this, Permissions.class));
        }else if(comms == null || comms.status == Comms.Status.DISCONNECTED){
            startComms();
            startActivity(new Intent(this, DeviceSelect.class));
        }else{
            comms.stop();
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
            onCommsError(R.string.fail_prepare);
            return;
        }
        comms.prep(requestData);
    }
    private boolean cantSendRequest(){
        return comms == null || comms.status == Comms.Status.DISCONNECTED || comms.status == Comms.Status.CONNECTING;
    }

    void sendSyncRequest(){
        if(cantSendRequest()) return;
        comms.sync();
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
                while((line = br.readLine()) != null) text.append(line);
                br.close();
                String matches_new_s = text.toString();
                JSONArray matches_new_ja = new JSONArray(matches_new_s);
                tabHistory.gotMatches(matches_new_ja);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "Main.importMatchesResult Exception: " + e.getMessage());
                onCommsError(R.string.fail_import);
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
                onCommsError(R.string.fail_export);
            }
        }
    );
}
