/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Activity{
    static final String LOG_TAG = "RugbyRefereeWatch";
    private static final String NOTIFICATION_CHANNEL_ID = "RugbyRefereeWatch_TimeUp";
    private static final String NOTIFICATION_CHANNEL_ID_ONGOING = "RugbyRefereeWatch_Ongoing";
    private static final long[] ve_waveForm = new long[]{300, 500, 300, 500, 300, 500};
    private static final VibrationEffect ve_pattern = VibrationEffect.createWaveform(ve_waveForm, -1);
    private static final VibrationEffect ve_single = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);

    static boolean isScreenRound;
    private boolean showSplash = true;
    private TextView battery;
    private TextView time;
    private RelativeLayout home;
    private RelativeLayout away;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private LinearLayout kickClockHome;
    private TextView tKickClockHome;
    private TextView tTimer;
    private LinearLayout kickClockAway;
    private TextView tKickClockAway;
    private View buttons_back;
    private Button bOverTimer;
    private Button bStart;
    private ImageButton bMatchLog;
    private Button bPenHome;
    private Button bPenAway;
    private Button bBottom;
    private ImageButton bConf;
    private ImageButton bConfWatch;
    private LinearLayout delay_end_wrapper;
    private CircularProgressIndicator delay_end_progress;
    private LinearLayout kick_clock_confirm;
    private TextView kick_clock_confirm_label;
    private ConfWatch confWatch;
    private Score score;
    private FoulPlay foulPlay;
    private PlayerNo playerNo;
    private ExtraTime extraTime;
    private Correct correct;
    private View touchView;

    private static int widthPixels;
    static int heightPixels;
    static int vh5;
    static int vh10;
    static int vh15;
    static int vh25;
    private static int vh45;
    private static int vh50;
    static int vh75;
    static int _10dp;

    //Timer
    enum TimerStatus{CONF, RUNNING, TIME_OFF, REST, READY, FINISHED}
    static TimerStatus timer_status = TimerStatus.CONF;
    private static long checkpoint_time = 0;//ms
    private static int checkpoint_duration = 0;//sec
    private static int checkpoint_previous = 0;//sec
    private static boolean timer_period_ended = false;
    static int timer_period = 0;
    static int timer_period_time = 2400;//sec
    private static final int TIMER_TYPE_UP = 0;
    static final int TIMER_TYPE_DOWN = 1;
    static int timer_type_period = TIMER_TYPE_DOWN;

    //Kick clocks
    private enum KickClockTypes{PK, CON, RESTART}
    private KickClockTypes kickClockType_home;
    private KickClockTypes kickClockType_away;
    private long kick_clock_home_end = -100;
    private long kick_clock_away_end = -100;

    //Settings
    static boolean screen_on = true;
    static int timer_type = TIMER_TYPE_DOWN;
    static boolean record_player = false;
    static boolean record_pens = false;
    static boolean delay_end = true;
    private final static int HELP_VERSION = 6;
    private static int battery_capacity = -1;

    static final MatchData match = new MatchData();
    private Handler handler;
    private static ExecutorService executorService;
    private Vibrator vibrator;
    private CommsBT commsBT;
    private BatteryManager batteryManager;
    private NotificationManager notificationManager;
    private DisplayManager displayManager;

    private static float onTouchStartY = -1;
    private static float onTouchStartX = 0;
    static long draggingEnded;
    private static int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;
    private static boolean hasBTPermission = false;
    private int delay_end_count;
    private long delay_end_start_time;

    @Override protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(()->showSplash);
        super.onCreate(savedInstanceState);
        isScreenRound = getResources().getConfiguration().isScreenRound();
        if(Build.VERSION.SDK_INT >= 30){
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
            widthPixels = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
            widthPixels = displayMetrics.widthPixels;
        }
        SWIPE_THRESHOLD = (int) (widthPixels*.3);
        vh5 = (int) (heightPixels*.05);
        vh10 = heightPixels/10;
        vh15 = (int) (heightPixels*.15);
        vh25 = (int) (heightPixels*.25);
        vh45 = (int) (heightPixels*.45);
        vh50 = heightPixels/2;
        vh75 = (int) (heightPixels*.75);
        int vw30 = (int) (widthPixels*.3);
        int vw50 = widthPixels/2;
        _10dp = getResources().getDimensionPixelSize(R.dimen._10dp);

        if(Build.VERSION.SDK_INT >= 31){
            vibrator = ((VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else{
            vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        }
        handler = new Handler(Looper.getMainLooper());
        commsBT = new CommsBT(this);
        batteryManager = (BatteryManager)getSystemService(BATTERY_SERVICE);
        displayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        checkNotificationChannels();

        setContentView(R.layout.main);

        // We need to listen for touch on all objects that have a click listener
        for(int id : new int[]{
                R.id.main, R.id.bConfWatch, R.id.home, R.id.score_home, R.id.away, R.id.score_away,
                R.id.tTimer, R.id.kickClockHome, R.id.kickClockAway,
                R.id.bPenHome, R.id.bPenAway, R.id.buttons_back, R.id.bOverTimer,
                R.id.bStart, R.id.bMatchLog, R.id.bBottom, R.id.bConf,
                R.id.kick_clock_confirm_no, R.id.kick_clock_confirm_yes,
                R.id.confWatch,
                R.id.score, R.id.score_try, R.id.score_con, R.id.score_goal,
                R.id.foulPlay, R.id.foul_play, R.id.card_yellow,
                R.id.penaltyTry, R.id.card_red,
                R.id.b_0, R.id.b_1, R.id.b_2, R.id.b_3, R.id.b_4, R.id.b_5, R.id.b_6, R.id.b_7,
                R.id.b_8, R.id.b_9, R.id.b_back, R.id.b_done,
                R.id.extraTime, R.id.extra_time_up, R.id.extra_time_2min, R.id.extra_time_5min,
                R.id.extra_time_10min,
                R.id.correct, R.id.svCorrect
        })findViewById(id).setOnTouchListener(this::onTouch);
        findViewById(R.id.main).setOnClickListener(v->onMainClick());

        battery = findViewById(R.id.battery);
        time = findViewById(R.id.time);
        home = findViewById(R.id.home);
        home.setOnClickListener(v->homeClick());
        away = findViewById(R.id.away);
        away.setOnClickListener(v->awayClick());
        score_home = findViewById(R.id.score_home);
        score_away = findViewById(R.id.score_away);
        sinbins_home = findViewById(R.id.sinbins_home);
        sinbins_away = findViewById(R.id.sinbins_away);
        kickClockHome = findViewById(R.id.kickClockHome);
        kickClockHome.setOnClickListener(v->kickClockHomeClick());
        tKickClockHome = findViewById(R.id.tKickClockHome);
        tTimer = findViewById(R.id.tTimer);
        tTimer.setOnClickListener(v->timerClick());
        kickClockAway = findViewById(R.id.kickClockAway);
        kickClockAway.setOnClickListener(v->kickClockAwayClick());
        tKickClockAway = findViewById(R.id.tKickClockAway);
        buttons_back = findViewById(R.id.buttons_back);
        bOverTimer = findViewById(R.id.bOverTimer);
        bOverTimer.setOnClickListener(v->bOverTimerClick());
        bStart = findViewById(R.id.bStart);
        bStart.setOnClickListener(v->bOverTimerClick());
        bBottom = findViewById(R.id.bBottom);
        bBottom.setOnClickListener(v->bBottomClick());
        kick_clock_confirm = findViewById(R.id.kick_clock_confirm);
        kick_clock_confirm_label = findViewById(R.id.kick_clock_confirm_label);
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v->startActivity(new Intent(this, ConfActivity.class)));
        confWatch = findViewById(R.id.confWatch);
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v->confWatch.show(this));
        delay_end_wrapper = findViewById(R.id.delay_end_wrapper);
        delay_end_progress = findViewById(R.id.delay_end_progress);
        findViewById(R.id.delay_end_cancel).setOnClickListener(v->delay_end_cancel());

        score = findViewById(R.id.score);
        score.onCreateMain(this);
        foulPlay = findViewById(R.id.foulPlay);
        foulPlay.onCreateMain(this);
        playerNo = findViewById(R.id.playerNo);
        playerNo.onCreateMain(this);
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v->correctClicked());
        correct.onCreateMain();
        bMatchLog = findViewById(R.id.bMatchLog);
        bMatchLog.setOnClickListener(v->startActivity(new Intent(this, MatchLog.class)));
        bPenHome = findViewById(R.id.bPenHome);
        bPenHome.setOnClickListener(v->bPenHomeClick());
        bPenAway = findViewById(R.id.bPenAway);
        bPenAway.setOnClickListener(v->bPenAwayClick());
        extraTime = findViewById(R.id.extraTime);
        extraTime.onCreateMain(this);

        //Resize elements for the heightPixels
        battery.setHeight(vh10);
        time.setHeight(vh15);
        score_home.setHeight(vh25);//will be resized to vh15 when sinbin is shown
        score_away.setHeight(vh25);
        findViewById(R.id.pen_label).getLayoutParams().height = vh15;
        buttons_back.getLayoutParams().height = vh45;
        bOverTimer.setHeight(vh25);
        bOverTimer.setPadding(0, vh5, 0, vh5);
        bStart.setHeight(vh25);
        bStart.setPadding(0, vh5, 0, vh5);
        bMatchLog.getLayoutParams().height = vh25;
        bMatchLog.setPadding(0, vh5, 0, vh5);
        bBottom.setHeight(vh25);
        bBottom.setPadding(0, vh5, 0, vh5);
        bConf.getLayoutParams().height = vh25;
        Button kick_clock_confirm_no = findViewById(R.id.kick_clock_confirm_no);
        kick_clock_confirm_no.getLayoutParams().width = vw50;
        kick_clock_confirm_no.setPadding(0, 0, 0, vh25);
        Button kick_clock_confirm_yes = findViewById(R.id.kick_clock_confirm_yes);
        kick_clock_confirm_yes.getLayoutParams().width = vw50;
        kick_clock_confirm_yes.setPadding(0, 0, 0, vh25);

        if(getResources().getConfiguration().fontScale > 1.1){
            battery.setIncludeFontPadding(false);
            time.setIncludeFontPadding(false);
            score_home.setIncludeFontPadding(false);
            score_away.setIncludeFontPadding(false);
            tTimer.setIncludeFontPadding(false);
            bStart.setIncludeFontPadding(false);
        }

        if(isScreenRound){
            score_home.setPadding(_10dp, 0, 0, 0);
            score_away.setPadding(0, 0, _10dp, 0);
            tTimer.setPadding(vh10, 0, vh10, 0);
            int _5dp = getResources().getDimensionPixelSize(R.dimen._5dp);
            bPenHome.setPadding(vw30, 0, 0, _5dp);
            bPenAway.setPadding(0, 0, vw30, _5dp);
            findViewById(R.id.pen_label).setPadding(0, 0, 0, _5dp);
        }

        if(timer_status == TimerStatus.CONF){
            checkPermissions();
            runInBackground(()->FileStore.readSettings(this));
            runInBackground(()->FileStore.cleanMatches(this));
        }else{
            updateScore();
        }

        updateBattery.run();
        updateTime.run();
        update.run();
        updateAfterConfig();
        startBT();
        showSplash = false;
    }
    @Override public void onResume(){
        super.onResume();
        updateAfterConfig();
    }
    @Override public void onDestroy(){
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        runInBackground(()->{
            if(commsBT != null) commsBT.stopBT();
            commsBT = null;
        });
        stopOngoingNotification();
    }

    private void checkPermissions(){
        if(Build.VERSION.SDK_INT >= 33){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
            if(!hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                    || !hasBTPermission
            ){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        }else if(Build.VERSION.SDK_INT >= 31){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
        }else{
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH}, 1);
            }
        }
    }
    private boolean hasPermission(String permission){
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                    permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    hasBTPermission = true;
                    startBT();
                }
                return;
            }
        }
    }

    static void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            onBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void onBack(){
        if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(extraTime.getVisibility() == View.VISIBLE){
            extraTime.setVisibility(View.GONE);
        }else if(playerNo.getVisibility() == View.VISIBLE){
            playerNo.setVisibility(View.GONE);
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            foulPlay.setVisibility(View.GONE);
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(confWatch.getVisibility() == View.VISIBLE){
            confWatch.setVisibility(View.GONE);
            updateAfterConfig();
            runInBackground(()->FileStore.storeSettings(this));
        }else if(kick_clock_confirm.getVisibility() == View.VISIBLE){
            kick_clock_confirm.setVisibility(View.GONE);
        }else{
            if(timer_status == TimerStatus.CONF || timer_status == TimerStatus.FINISHED){
                finish();
            }else{
                correct.show(this);
            }
        }
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent ev){
        super.dispatchGenericMotionEvent(ev);
        return true; //Just to let Google know we are listening to rotary events
    }

    private void onMainClick(){
        //We need to do this to make sure that we can listen for onTouch on main
        Log.i(LOG_TAG, "onMainClick");
    }
    void addOnTouch(View v){v.setOnTouchListener(this::onTouch);}
    private boolean onTouch(View ignoredV, MotionEvent event){
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                onTouchInit(event);
                super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if(onTouchStartY == -1) onTouchInit(event);
                if(touchView == null) return false;

                int diffX1 = getBackSwipeDiffX(event);
                if(getBackSwipeVelocity(event, diffX1) < SWIPE_VELOCITY_THRESHOLD){
                    touchView.animate()
                            .x(0)
                            .scaleX(1f).scaleY(1f)
                            .setDuration(300).start();
                }else if(diffX1 > 0){
                    float move = event.getRawX() - onTouchStartX;
                    float scale = 1 - move/widthPixels;
                    if(isScreenRound){
                        touchView.setBackgroundResource(R.drawable.bg_round);
                    }
                    touchView.animate().x(move)
                            .scaleX(scale).scaleY(scale)
                            .setDuration(0).start();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(touchView != null){
                    touchView.animate()
                            .x(0)
                            .scaleX(1f).scaleY(1f)
                            .setDuration(150).start();
                    if(isScreenRound){
                        touchView.setBackgroundResource(0);
                        touchView.setBackgroundColor(getResources().getColor(R.color.black, null));
                    }
                }
                int diffX2 = getBackSwipeDiffX(event);
                float velocity2 = getBackSwipeVelocity(event, diffX2);
                onTouchStartY = -1;
                if(diffX2 > SWIPE_THRESHOLD && velocity2 > SWIPE_VELOCITY_THRESHOLD){
                    if(draggingEnded+100 < System.currentTimeMillis()) onBack();
                    draggingEnded = System.currentTimeMillis();
                    return true;
                }
        }
        return false;
    }
    private void onTouchInit(MotionEvent event){
        onTouchStartY = event.getRawY();
        onTouchStartX = event.getRawX();
        if(correct.getVisibility() == View.VISIBLE){
            touchView = correct;
        }else if(extraTime.getVisibility() == View.VISIBLE){
            touchView = extraTime;
        }else if(playerNo.getVisibility() == View.VISIBLE){
            touchView = playerNo;
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            touchView = foulPlay;
        }else if(score.getVisibility() == View.VISIBLE){
            touchView = score;
        }else if(confWatch.getVisibility() == View.VISIBLE){
            touchView = confWatch;
        }else{
            touchView = null;
        }
    }

    private int getBackSwipeDiffX(MotionEvent event){
        float diffY = event.getRawY() - onTouchStartY;
        float diffX = event.getRawX() - onTouchStartX;
        if(diffX > 0 && Math.abs(diffX) > Math.abs(diffY)) return Math.round(diffX);
        return -1;
    }
    private float getBackSwipeVelocity(MotionEvent event, float diffX){
        return (diffX / (event.getEventTime() - event.getDownTime())) * 1000;
    }

    private void startBT(){
        if(!hasBTPermission ||
                timer_status == TimerStatus.RUNNING ||
                timer_status == TimerStatus.TIME_OFF ||
                timer_status == TimerStatus.REST ||
                timer_status == TimerStatus.READY) return;
        runInBackground(commsBT::startBT);
    }

    private void timerClick(){
        if(timer_status == TimerStatus.RUNNING){
            singleBeep();
            long timestamp = System.currentTimeMillis();
            checkpoint_duration = getDurationPeriod(timestamp);
            checkpoint_time = timestamp;
            timer_status = TimerStatus.TIME_OFF;
            match.logEvent("TIME OFF", null, 0);
            updateButtons();
            handler.postDelayed(timeOffBuzz, 15000);
        }
    }
    private void bOverTimerClick(){
        switch(timer_status){
            case CONF:
                match.match_id = System.currentTimeMillis();
                runInBackground(commsBT::stopBT);
            case READY:
                singleBeep();
                timer_status = TimerStatus.RUNNING;
                checkpoint_time = System.currentTimeMillis();
                checkpoint_duration = 0;
                String kickoffTeam = getKickoffTeam();//capture before increasing timer_period
                timer_period++;
                match.logEvent("START", kickoffTeam, 0);
                updateScore();
                startOngoingNotification();
                break;
            case TIME_OFF:
                //resume running
                singleBeep();
                timer_status = TimerStatus.RUNNING;
                checkpoint_time = System.currentTimeMillis();
                match.logEvent("RESUME", null, 0);
                break;
            case REST:
                //get ready for next period
                timer_status = TimerStatus.READY;
                timer_type_period = timer_type;
                break;
            case FINISHED:
                Report.match = match;
                startActivity(new Intent(this, Report.class));
                break;
            default://ignore
                return;
        }
        updateButtons();
    }
    private void bBottomClick(){
        if(draggingEnded+1000 > System.currentTimeMillis()) return;
        draggingEnded = System.currentTimeMillis();

        switch(timer_status){
            case TIME_OFF:
                delay_end_start();
                break;
            case REST:
                timer_status = TimerStatus.FINISHED;
                timer_period_time = match.period_time*60;
                timer_type_period = timer_type;
                updateScore();

                runInBackground(()->FileStore.storeMatch(this));
                startBT();
                stopOngoingNotification();
                updateButtons();
                break;
            case FINISHED:
                timer_status = TimerStatus.CONF;
                checkpoint_time = 0;
                checkpoint_duration = 0;
                checkpoint_previous = 0;
                timer_period_ended = false;
                timer_period = 0;
                match.clear();
                updateScore();
                updateAfterConfig();
                updateSinbins();
                updateButtons();
                break;
            case READY:
                extraTime.setVisibility(View.VISIBLE);
                break;
        }
    }
    private void updateButtons(){
        String bOverTimerText;
        String bBottomText;
        switch(timer_status){
            case CONF:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
                bStart.setVisibility(View.VISIBLE);
                bMatchLog.setVisibility(View.VISIBLE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.VISIBLE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case READY:
                bConfWatch.setVisibility(View.GONE);
                bOverTimerText = getString(R.string.start) + " ";
                if(match.period_count == 2 && timer_period == 1){
                    bBottom.setVisibility(View.GONE);
                    bOverTimerText += getString(R.string._2nd_half);
                }else if(timer_period >= match.period_count){
                    extraTimeChange(0);
                    bBottom.setVisibility(View.VISIBLE);
                    bOverTimerText += getPeriodName(timer_period+1);
                }else{
                    bBottom.setVisibility(View.GONE);
                    bOverTimerText += getPeriodName(timer_period+1);
                }
                bOverTimer.setText(bOverTimerText);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case TIME_OFF:
                bConfWatch.setVisibility(View.VISIBLE);
                bOverTimer.setText(R.string.resume);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);

                bBottomText = getString(R.string.end) + " ";
                if(match.period_count == 2 && timer_period == 1){
                    bBottomText = getString(R.string.half_time);
                }else if(match.period_count == 2 && timer_period == 2){
                    bBottomText = getString(R.string.full_time);
                }else if(timer_period > match.period_count){
                    if(timer_period == match.period_count+1){
                        bBottomText += getString(R.string.extra);
                    }else{
                        bBottomText += getString(R.string.extra) + " " + (timer_period-match.period_count);
                    }
                }else{
                    bBottomText = getString(R.string.rest);
                }
                bBottom.setText(bBottomText);
                bBottom.setVisibility(View.VISIBLE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case REST:
                bConfWatch.setVisibility(View.VISIBLE);
                if(match.period_count == 2 && timer_period == 1){
                    bOverTimerText = getString(R.string._2nd_half);
                }else if(timer_period >= match.period_count){
                    bOverTimerText = getPeriodName(timer_period+1);
                }else{
                    bOverTimerText = getString(R.string.period) + " " + (timer_period+1);
                }
                bOverTimer.setText(bOverTimerText);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setText(R.string.finish);
                bBottom.setVisibility(View.VISIBLE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case FINISHED:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setText(R.string.report);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setText(R.string.clear);
                bBottom.setVisibility(View.VISIBLE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            default:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.INVISIBLE);
        }
        updateTimer();
    }
    private void kickClockHomeShow(int label_text, int secs){
        if(secs == 0) return;
        kickClockAwayDone();
        ((TextView)findViewById(R.id.tKickClockHomeLabel)).setText(label_text);
        kick_clock_home_end = getDurationFull() + secs;
        tKickClockHome.setText(String.valueOf(secs));
        kickClockHome.setVisibility(View.VISIBLE);
        if(isScreenRound) tTimer.setPadding(0, 0, vh10, 0);
    }
    private void kickClockAwayShow(int label_text, int secs){
        if(secs == 0) return;
        kickClockHomeDone();
        ((TextView)findViewById(R.id.tKickClockAwayLabel)).setText(label_text);
        kick_clock_away_end = getDurationFull() + secs;
        tKickClockAway.setText(String.valueOf(secs));
        kickClockAway.setVisibility(View.VISIBLE);
        if(isScreenRound) tTimer.setPadding(vh10, 0, 0, 0);
    }
    private void kickClockHomeClick(){
        switch(kickClockType_home){
            case CON:
                kick_clock_confirm_label.setText(R.string.kc_confirm_conv);
                break;
            case PK:
                kick_clock_confirm_label.setText(R.string.kc_confirm_pk);
                break;
            case RESTART:
                kickClockHomeDone();
                return;
        }
        findViewById(R.id.kick_clock_confirm_no).setOnClickListener(v->{
            kickClockHomeDone();
            kick_clock_confirm.setVisibility(View.GONE);
        });
        findViewById(R.id.kick_clock_confirm_yes).setOnClickListener(v->{
            score.team = match.home;
            switch(kickClockType_home){
                case CON:
                    conversionClick();
                    break;
                case PK:
                    goalClick();
                    break;
            }
            kick_clock_confirm.setVisibility(View.GONE);
        });
        kick_clock_confirm.setVisibility(View.VISIBLE);
    }
    private void kickClockAwayClick(){
        switch(kickClockType_away){
            case CON:
                kick_clock_confirm_label.setText(R.string.kc_confirm_conv);
                break;
            case PK:
                kick_clock_confirm_label.setText(R.string.kc_confirm_pk);
                break;
            case RESTART:
                kickClockAwayDone();
                return;
        }
        findViewById(R.id.kick_clock_confirm_no).setOnClickListener(v->{
            kickClockAwayDone();
            kick_clock_confirm.setVisibility(View.GONE);
        });
        findViewById(R.id.kick_clock_confirm_yes).setOnClickListener(v->{
            score.team = match.away;
            switch(kickClockType_away){
                case CON:
                    conversionClick();
                    break;
                case PK:
                    goalClick();
                    break;
            }
            kick_clock_confirm.setVisibility(View.GONE);
        });
        kick_clock_confirm.setVisibility(View.VISIBLE);
    }
    private void kickClockHomeDone(){
        if(timer_status == TimerStatus.RUNNING && kickClockType_home == KickClockTypes.CON && match.clock_restart > 0){
            kickClockType_home = KickClockTypes.RESTART;
            kickClockHomeShow(R.string.restart, match.clock_restart);
            return;
        }
        kickClockHomeHide();
    }
    void kickClockHomeHide(){
        kick_clock_home_end = -100;
        kickClockHome.setVisibility(View.GONE);
        if(isScreenRound) tTimer.setPadding(vh10, 0, vh10, 0);
    }
    private void kickClockAwayDone(){
        if(timer_status == TimerStatus.RUNNING && kickClockType_away == KickClockTypes.CON && match.clock_restart > 0){
            kickClockType_away = KickClockTypes.RESTART;
            kickClockAwayShow(R.string.restart, match.clock_restart);
            return;
        }
        kickClockAwayHide();
    }
    void kickClockAwayHide(){
        kick_clock_away_end = -100;
        kickClockAway.setVisibility(View.GONE);
        if(isScreenRound) tTimer.setPadding(vh10, 0, vh10, 0);
    }

    private void delay_end_start(){
        delay_end_start_time = System.currentTimeMillis();
        if(!delay_end){
            endPeriod();
            return;
        }
        delay_end_count = 11;
        delay_end_update.run();
        delay_end_wrapper.setVisibility(View.VISIBLE);
        delay_end_progress.setProgress(0);
        delay_end_progress.setVisibility(View.VISIBLE);
    }
    private void delay_end_cancel(){
        delay_end_count = -1;
        delay_end_wrapper.setVisibility(View.GONE);
        delay_end_progress.setVisibility(View.GONE);
    }
    private final Runnable delay_end_update = new Runnable(){@Override public void run(){
        if(delay_end_count == -1) return;
        delay_end_count--;
        if(delay_end_count == 0){
            endPeriod();
        }
        delay_end_progress.setProgress((10-delay_end_count)*10);
        handler.postDelayed(delay_end_update, 1000);
    }};
    private void endPeriod(){
        //How did someone get here with no events in the match?
        if(!match.events.isEmpty() && match.events.get(match.events.size()-1).what.equals("TIME OFF")){
            match.events.remove(match.events.size()-1);
        }
        match.logEvent("END", null, delay_end_start_time);

        int correct = timer_period_time - getDurationPeriod(delay_end_start_time);
        match.home.sinbins.forEach(sb->sb.end += correct);
        match.away.sinbins.forEach(sb->sb.end += correct);

        timer_status = TimerStatus.REST;
        timer_period_ended = false;
        timer_type_period = TIMER_TYPE_UP;
        checkpoint_duration = 0;
        checkpoint_time = delay_end_start_time;
        checkpoint_previous += timer_period_time;
        tTimer.setTextColor(getResources().getColor(R.color.white, getTheme()));

        String kickoffTeam = getKickoffTeam();
        if(kickoffTeam != null){
            if(kickoffTeam.equals(MatchData.HOME_ID)){
                kickoffTeam = match.home.tot + " " + getString(R.string.kick);
                score_home.setText(kickoffTeam);
            }else{
                kickoffTeam = match.away.tot + " " + getString(R.string.kick);
                score_away.setText(kickoffTeam);
            }
        }
        updateButtons();
        if(kick_clock_home_end > 0) kickClockHomeDone();
        if(kick_clock_away_end > 0) kickClockAwayDone();
        delay_end_wrapper.setVisibility(View.GONE);
        delay_end_progress.setVisibility(View.GONE);
    }

    private int getColorBG(String name){
        return switch(name){
            case "black" -> getResources().getColor(R.color.black, null);
            case "blue" -> getResources().getColor(R.color.blue, null);
            case "brown" -> getResources().getColor(R.color.brown, null);
            case "gold" -> getResources().getColor(R.color.gold, null);
            case "green" -> getResources().getColor(R.color.green, null);
            case "orange" -> getResources().getColor(R.color.orange, null);
            case "pink" -> getResources().getColor(R.color.pink, null);
            case "purple" -> getResources().getColor(R.color.purple, null);
            case "red" -> getResources().getColor(R.color.red, null);
            case "white" -> getResources().getColor(R.color.white, null);
            default -> getResources().getColor(R.color.black, null);
        };
    }
    private int getColorFG(String name){
        return switch(name){
            case "gold", "green", "orange", "pink", "white" ->
                    getResources().getColor(R.color.black, null);
            default -> //black blue brown purple red
                    getResources().getColor(R.color.white, null);
        };
    }
    void updateAfterConfig(){//Thread: Always on UI thread
        updateTimer();

        home.setBackgroundColor(getColorBG(match.home.color));
        score_home.setTextColor(getColorFG(match.home.color));
        away.setBackgroundColor(getColorBG(match.away.color));
        score_away.setTextColor(getColorFG(match.away.color));

        if(screen_on){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if(Main.record_pens){
            bPenHome.setVisibility(View.VISIBLE);
            findViewById(R.id.pen_label).setVisibility(View.VISIBLE);
            bPenAway.setVisibility(View.VISIBLE);
            ((ConstraintLayout.LayoutParams)tTimer.getLayoutParams()).bottomMargin = 0;
            if(isScreenRound){
                kickClockHome.setPadding(vh10, 0, _10dp, vh5);
                kickClockAway.setPadding(_10dp, 0, vh10, vh5);
            }
        }else{
            bPenHome.setVisibility(View.GONE);
            findViewById(R.id.pen_label).setVisibility(View.GONE);
            bPenAway.setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams)tTimer.getLayoutParams()).bottomMargin = vh10;
            if(isScreenRound){
                kickClockHome.setPadding(vh10, 0, _10dp, vh10);
                kickClockAway.setPadding(_10dp, 0, vh10, vh10);
            }
        }
        score.update();
    }
    private final Runnable updateBattery = new Runnable(){@Override public void run(){
        runInBackground(()->{
            battery_capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            String text;
            if(battery_capacity < 0){
                text = "---%";
                handler.postDelayed(updateBattery, 100);
            }else{
                text = battery_capacity + "%";
                handler.postDelayed(updateBattery, 30000);
            }
            runOnUiThread(()->battery.setText(text));
        });
    }};
    private final Runnable updateTime = new Runnable(){@Override public void run(){
        time.setText(Utils.prettyTime());
        handler.postDelayed(updateTime, 1000-(System.currentTimeMillis()%1000));
    }};
    private final Runnable update = new Runnable(){@Override public void run(){
        switch(timer_status){
            case RUNNING:
                updateKickClocks();
                updateSinbins();
            case REST:
                updateTimer();
                break;
        }
        handler.postDelayed(update, 1000-((System.currentTimeMillis()-checkpoint_time)%1000));
    }};

    private void updateTimer(){
        int durationPeriod = getDurationPeriod();
        if(timer_status == TimerStatus.REST){
            tTimer.setText(Utils.prettyTimer(durationPeriod));
        }else if(timer_type_period == TIMER_TYPE_DOWN){
            tTimer.setText(Utils.prettyTimer(timer_period_time - durationPeriod));
        }else{
            tTimer.setText(Utils.prettyTimer(getDurationFull()));
        }
        if(!timer_period_ended && timer_status == TimerStatus.RUNNING && durationPeriod > timer_period_time){
            timer_period_ended = true;
            tTimer.setTextColor(getResources().getColor(R.color.red, getTheme()));
            beep(getString(R.string.ended, getPeriodName(timer_period)));
        }
    }
    private void updateKickClocks(){
        if(kick_clock_home_end > 0){
            long kick_clock = kick_clock_home_end - getDurationFull();
            tKickClockHome.setText(kick_clock < 0 ? "0" : String.valueOf(kick_clock));
            if(kick_clock < -10 || (kick_clock < 0 && (kickClockType_home == KickClockTypes.CON && match.clock_restart > 0)))
                kickClockHomeDone();
        }
        if(kick_clock_away_end > 0){
            long kick_clock = kick_clock_away_end - getDurationFull();
            tKickClockAway.setText(kick_clock < 0 ? "0" : String.valueOf(kick_clock));
            if(kick_clock < -10 || (kick_clock < 0 && (kickClockType_away == KickClockTypes.CON && match.clock_restart > 0)))
                kickClockAwayDone();
        }
    }
    void updateSinbins(){
        updateSinbins(match.home, al_sinbins_ui_home, sinbins_home);
        updateSinbins(match.away, al_sinbins_ui_away, sinbins_away);
    }
    private final ArrayList<Sinbin> al_sinbins_ui_home = new ArrayList<>();
    private final ArrayList<Sinbin> al_sinbins_ui_away = new ArrayList<>();
    private void updateSinbins(MatchData.Team team, ArrayList<Sinbin> al_sinbins_ui, LinearLayout llSinbins){
        for(MatchData.Sinbin sinbin_data : team.sinbins){
            boolean exists = false;
            for(Sinbin sinbin_ui : al_sinbins_ui){
                if(sinbin_data.id == sinbin_ui.sinbin.id){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                Sinbin sb = new Sinbin(this, sinbin_data, getColorFG(team.color));
                llSinbins.addView(sb);
                al_sinbins_ui.add(sb);
                if(team.isHome()){
                    score_home.setHeight(vh15);
                }else{
                    score_away.setHeight(vh15);
                }
                if(al_sinbins_ui_home.size() > 1 || al_sinbins_ui_away.size() > 1){
                    buttons_back.getLayoutParams().height = vh45;
                }else{
                    buttons_back.getLayoutParams().height = vh50;
                }
            }
        }
        for(int i = al_sinbins_ui.size(); i > 0; i--){
            Sinbin sinbin_ui = al_sinbins_ui.get(i-1);
            if(!team.hasSinbin(sinbin_ui.sinbin.id) || sinbin_ui.sinbin.hide){
                llSinbins.removeView(sinbin_ui);
                al_sinbins_ui.remove(sinbin_ui);
                if(al_sinbins_ui.isEmpty()){
                    if(team.isHome()){
                        score_home.setHeight(vh25);
                    }else{
                        score_away.setHeight(vh25);
                    }
                }
                if(al_sinbins_ui_home.size() > 1 || al_sinbins_ui_away.size() > 1){
                    buttons_back.getLayoutParams().height = vh45;
                }else{
                    buttons_back.getLayoutParams().height = vh50;
                }
            }else{
                sinbin_ui.update();
            }
        }
    }
    private static int getDurationPeriod(long timestamp){
        if(timer_status == TimerStatus.RUNNING || timer_status == TimerStatus.REST)
            return (int)(Math.floorDiv(timestamp - checkpoint_time, 1000) + checkpoint_duration);
        return checkpoint_duration;
    }
    private static int getDurationPeriod(){
        return getDurationPeriod(System.currentTimeMillis());
    }
    static int getDurationFull(long timestamp){
        return getDurationPeriod(timestamp) + checkpoint_previous;
    }
    static int getDurationFull(){
        if(timer_status == TimerStatus.RUNNING)
            return getDurationFull(System.currentTimeMillis());
        return checkpoint_duration + checkpoint_previous;
    }

    private void bPenHomeClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.home.pens++;
        updateScore();
        match.logEvent("PENALTY", MatchData.HOME_ID, 0);
        kickClockType_away = KickClockTypes.PK;
        kickClockAwayShow(R.string.pk, match.clock_pk);
    }
    private void bPenAwayClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.away.pens++;
        updateScore();
        match.logEvent("PENALTY", MatchData.AWAY_ID, 0);
        kickClockType_home = KickClockTypes.PK;
        kickClockHomeShow(R.string.pk, match.clock_pk);
    }
    private void homeClick(){
        if(timer_status == TimerStatus.CONF){
            if(match.home.kickoff){
                match.home.kickoff = false;
                score_home.setText("0");
            }else{
                match.home.kickoff = true;
                score_home.setText(R.string._0_kick);
            }
            match.away.kickoff = false;
            score_away.setText("0");
        }else if(timer_status == TimerStatus.FINISHED){
            Log.d(LOG_TAG, "homeClick, but in status FINISHED");
        }else{
            score.team = match.home;
            score.setVisibility(View.VISIBLE);
        }
    }
    private void awayClick(){
        if(timer_status == TimerStatus.CONF){
            if(match.away.kickoff){
                match.away.kickoff = false;
                score_away.setText("0");
            }else{
                match.away.kickoff = true;
                score_away.setText(R.string._0_kick);
            }
            match.home.kickoff = false;
            score_home.setText("0");
        }else if(timer_status == TimerStatus.FINISHED){
            Log.d(LOG_TAG, "awayClick, but in status FINISHED");
        }else{
            score.team = match.away;
            score.setVisibility(View.VISIBLE);
        }
    }
    void tryClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.tries++;
        updateScore();
        MatchData.Event event = match.logEvent("TRY", score.team.id, 0);
        if(record_player) playerNo.show(event, null);
        if(score.team.isHome()){
            kickClockType_home = KickClockTypes.CON;
            kickClockHomeShow(R.string.conversion, match.clock_con);
        }else{
            kickClockType_away = KickClockTypes.CON;
            kickClockAwayShow(R.string.conversion, match.clock_con);
        }
        score.setVisibility(View.GONE);
    }
    void conversionClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.cons++;
        updateScore();
        MatchData.Event event = match.logEvent("CONVERSION", score.team.id, 0);
        if(record_player) playerNo.show(event, null);
        if(score.team.isHome()){
            kickClockHomeDone();
        }else{
            kickClockAwayDone();
        }
        score.setVisibility(View.GONE);
    }
    void goalClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.goals++;
        updateScore();
        MatchData.Event event = match.logEvent("GOAL", score.team.id, 0);
        if(record_player) playerNo.show(event, null);
        if(score.team.isHome()){
            kickClockHomeDone();
        }else{
            kickClockAwayDone();
        }
        score.setVisibility(View.GONE);
    }
    private void updateScore(){
        match.home.tot = match.home.tries*match.points_try +
                match.home.cons*match.points_con +
                match.home.pen_tries*(match.points_try + match.points_con) +
                match.home.goals*match.points_goal;
        score_home.setText(String.valueOf(match.home.tot));
        match.away.tot = match.away.tries*match.points_try +
                match.away.cons*match.points_con +
                match.away.pen_tries*(match.points_try + match.points_con) +
                match.away.goals*match.points_goal;
        score_away.setText(String.valueOf(match.away.tot));

        bPenHome.setText(String.valueOf(match.home.pens));
        bPenAway.setText(String.valueOf(match.away.pens));
    }
    void foulPlayClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    void penaltyTryClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.pen_tries++;
        updateScore();
        match.logEvent("PENALTY TRY", score.team.id, 0);
        foulPlay.setVisibility(View.GONE);
    }
    void card_yellowClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        long timestamp = System.currentTimeMillis();
        MatchData.Event event = match.logEvent("YELLOW CARD", score.team.id, timestamp);
        MatchData.Sinbin sinbin = score.team.addSinbin(
                timestamp,
                getDurationFull(timestamp)+(match.sinbin*60),
                score.team.id
        );
        updateSinbins();
        score.team.yellow_cards++;
        score.team.pens++;
        updateScore();
        playerNo.show(event, sinbin);
        foulPlay.setVisibility(View.GONE);
    }
    void card_redClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        MatchData.Event event = match.logEvent("RED CARD", score.team.id, 0);
        playerNo.show(event, null);
        score.team.red_cards++;
        score.team.pens++;
        updateScore();
        foulPlay.setVisibility(View.GONE);
    }

    private void correctClicked(){
        updateScore();
        updateSinbins();
    }

    private String getPeriodName(int period){
        return getPeriodName(this, period, match.period_count);
    }
    static String getPeriodName(Context context, int period, int period_count){
        if(period > period_count){
            if(period == period_count+1){
                return context.getString(R.string.extra_time);
            }else{
                return context.getString(R.string.extra_time) + " " + (period - period_count);
            }
        }else if(period_count == 2){
            return switch(period){
                case 1 -> context.getString(R.string.first_half);
                case 2 -> context.getString(R.string.second_half);
                default -> String.valueOf(period);
            };
        }else{
            return switch(period){
                case 1 -> context.getString(R.string._1st);
                case 2 -> context.getString(R.string._2nd);
                case 3 -> context.getString(R.string._3rd);
                case 4 -> context.getString(R.string._4th);
                default -> String.valueOf(period);
            };
        }
    }
    private String getKickoffTeam(){
        if(match.home.kickoff){
            if(timer_period % 2 == 0){
                return MatchData.HOME_ID;
            }else{
                return MatchData.AWAY_ID;
            }
        }
        if(match.away.kickoff){
            if(timer_period % 2 == 0){
                return MatchData.AWAY_ID;
            }else{
                return MatchData.HOME_ID;
            }
        }
        return null;
    }

    boolean incomingSettings(JSONObject settings){//Thread: Background thread
        if(timer_status != TimerStatus.CONF) return false;
        try{
            match.home.team = settings.getString("home_name");
            match.home.color = settings.getString("home_color");
            match.away.team = settings.getString("away_name");
            match.away.color = settings.getString("away_color");
            match.match_type = settings.getString("match_type");
            match.period_time = settings.getInt("period_time");
            timer_period_time = match.period_time*60;
            match.period_count = settings.getInt("period_count");
            match.sinbin = settings.getInt("sinbin");
            match.points_try = settings.getInt("points_try");
            match.points_con = settings.getInt("points_con");
            match.points_goal = settings.getInt("points_goal");
            match.clock_pk = settings.has("clock_pk") ? settings.getInt("clock_pk") : 0;//March 2025
            match.clock_con = settings.has("clock_con") ? settings.getInt("clock_con") : 0;//March 2025
            match.clock_restart = settings.has("clock_restart") ? settings.getInt("clock_restart") : 0;//March 2025

            if(settings.has("screen_on"))
                screen_on = settings.getBoolean("screen_on");
            if(settings.has("timer_type")){
                timer_type = settings.getInt("timer_type");
                timer_type_period = timer_type;
            }
            if(settings.has("record_player"))
                record_player = settings.getBoolean("record_player");
            if(settings.has("record_pens"))
                record_pens = settings.getBoolean("record_pens");
            if(settings.has("delay_end"))
                delay_end = settings.getBoolean("delay_end");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.incomingSettings Exception: " + e.getMessage());
            runOnUiThread(()->Toast.makeText(this, R.string.fail_receive_settings, Toast.LENGTH_SHORT).show());
            return false;
        }
        return true;
    }

    void readSettings(JSONObject jsonSettings){//Thread: Background thread
        if(timer_status != TimerStatus.CONF) return;
        try{
            match.home.color = jsonSettings.getString("home_color");
            match.away.color = jsonSettings.getString("away_color");

            match.match_type = jsonSettings.getString("match_type");
            match.period_time = jsonSettings.getInt("period_time");
            timer_period_time = match.period_time*60;
            match.period_count = jsonSettings.getInt("period_count");
            match.sinbin = jsonSettings.getInt("sinbin");
            match.points_try = jsonSettings.getInt("points_try");
            match.points_con = jsonSettings.getInt("points_con");
            match.points_goal = jsonSettings.getInt("points_goal");
            if(jsonSettings.has("clock_pk"))//March 2025
                match.clock_pk = jsonSettings.getInt("clock_pk");
            if(jsonSettings.has("clock_con"))
                match.clock_con = jsonSettings.getInt("clock_con");
            if(jsonSettings.has("clock_restart"))
                match.clock_restart = jsonSettings.getInt("clock_restart");
            screen_on = jsonSettings.getBoolean("screen_on");
            timer_type = jsonSettings.getInt("timer_type");
            timer_type_period = timer_type;
            record_player = jsonSettings.getBoolean("record_player");
            record_pens = jsonSettings.getBoolean("record_pens");
            if(jsonSettings.has("delay_end"))//March 2025
                delay_end = jsonSettings.getBoolean("delay_end");

            if(jsonSettings.has("help_version") && HELP_VERSION != jsonSettings.getInt("help_version")){
                startActivity(new Intent(this, Help.class));
                runInBackground(()->FileStore.storeSettings(this));
            }
            runOnUiThread(this::updateAfterConfig);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.readSettings Exception: " + e.getMessage());
            runOnUiThread(()->Toast.makeText(this, R.string.fail_read_settings, Toast.LENGTH_SHORT).show());
        }
    }

    static JSONObject getSettings() throws JSONException{
        JSONObject settings = new JSONObject();
        settings.put("home_name", match.home.team);
        settings.put("home_color", match.home.color);
        settings.put("away_name", match.away.team);
        settings.put("away_color", match.away.color);
        settings.put("match_type", match.match_type);
        settings.put("period_time", match.period_time);
        settings.put("period_count", match.period_count);
        settings.put("sinbin", match.sinbin);
        settings.put("points_try", match.points_try);
        settings.put("points_con", match.points_con);
        settings.put("points_goal", match.points_goal);
        settings.put("clock_pk", match.clock_pk);
        settings.put("clock_con", match.clock_con);
        settings.put("clock_restart", match.clock_restart);
        settings.put("screen_on", screen_on);
        settings.put("timer_type", timer_type);
        settings.put("record_player", record_player);
        settings.put("record_pens", record_pens);
        settings.put("delay_end", delay_end);
        settings.put("help_version", HELP_VERSION);
        return settings;
    }

    void extraTimeChange(int time){
        if(time == 0){
            bBottom.setText(R.string.count_up);
            timer_type_period = TIMER_TYPE_UP;
            timer_period_time = match.period_time*60;
        }else{
            bBottom.setText(String.format("%s %s", time, getString(R.string.min)));
            timer_type_period = timer_type;
            timer_period_time = time*60;
        }
        updateTimer();
        extraTime.setVisibility(View.GONE);
    }

    void beep(String notificationText){
        for(Display display : displayManager.getDisplays()){
            if(display.getState() != Display.STATE_OFF){
                vibrator.vibrate(ve_pattern);
                return;
            }
        }
        notificationManager.notify(2, new Notification
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_foreground)
                .setContentTitle(notificationText)
                .build()
        );
        handler.postDelayed(()->notificationManager.cancel(2), 60000);
    }
    private void singleBeep(){vibrator.vibrate(ve_single);}
    private final Runnable timeOffBuzz = new Runnable(){@Override public void run(){
        if(timer_status == TimerStatus.TIME_OFF){
            beep(getString(R.string.time_off_reminder));
            handler.postDelayed(timeOffBuzz, 15000);
            handler.postDelayed(()->notificationManager.cancel(2), 5000);
        }
    }};

    private void startOngoingNotification(){
        if(Build.VERSION.SDK_INT < 30) return;
        Intent actionIntent = new Intent(this, Main.class);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(
                this,
                0,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder notificationBuilder = new NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID_ONGOING)
                .setSmallIcon(R.drawable.icon_foreground)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    R.drawable.icon_foreground,
                    getString(R.string.open_rrw),
                    actionPendingIntent
                )
                .setOngoing(true);
        Status ongoingActivityStatus = new Status
                .Builder()
                .addTemplate(getString(R.string.match_ongoing))
                .build();
        OngoingActivity ongoingActivity = new OngoingActivity
                .Builder(
                    this,
                    1,
                    notificationBuilder
                )
                .setStaticIcon(R.drawable.icon_foreground)
                .setTouchIntent(actionPendingIntent)
                .setStatus(ongoingActivityStatus)
                .build();
        ongoingActivity.apply(this);
        notificationManager.notify(1, notificationBuilder.build());
    }
    private void stopOngoingNotification(){
        if(Build.VERSION.SDK_INT < 30) return;
        notificationManager.cancelAll();
    }
    private void checkNotificationChannels(){
        boolean exists = false;
        boolean exists_ongoing = false;
        for(NotificationChannel channel : notificationManager.getNotificationChannels()){
            String channelId = channel.getId();
            switch(channelId){
                case NOTIFICATION_CHANNEL_ID:
                    exists = true;
                    continue;
                case NOTIFICATION_CHANNEL_ID_ONGOING:
                    exists_ongoing = true;
                    continue;
            }
            notificationManager.deleteNotificationChannel(channelId);
        }
        if(!exists){
            NotificationChannel channel =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            getString(R.string.time_up),
                            NotificationManager.IMPORTANCE_HIGH
                    );
            channel.enableVibration(true);
            channel.setVibrationPattern(ve_waveForm);
            notificationManager.createNotificationChannel(channel);
        }
        if(!exists_ongoing){
            notificationManager.createNotificationChannel(
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID_ONGOING,
                            getString(R.string.match_ongoing),
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            );
        }
    }
}
