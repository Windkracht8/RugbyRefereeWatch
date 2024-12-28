package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Activity{
    static final String LOG_TAG = "RugbyRefereeWatch";
    static boolean isScreenRound;
    private boolean showSplash = true;
    private boolean isPenPadInitialized = false;
    private TextView battery;
    private TextView time;
    private RelativeLayout home;
    private RelativeLayout away;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private TextView tTimer;
    private View buttons_back;
    private Button bOverTimer;
    private Button bStart;
    private ImageButton bMatchLog;
    private TextView pen_label;
    private Button bPenHome;
    private Button bPenAway;
    private Button bBottom;
    private ImageButton bConf;
    private ImageButton bConfWatch;
    private LinearLayout confirm;
    private TextView confirm_text;
    private Conf conf;
    private ConfWatch confWatch;
    private Score score;
    private FoulPlay foulPlay;
    private ExtraTime extraTime;
    private Correct correct;
    private Report report;
    private MatchLog matchLog;
    private Help help;
    private View touchView;

    private static int widthPixels;
    static int vh5;
    static int vh10;
    static int vh15;
    static int vh25;
    static int vh30;
    private static int vh40;
    static int _10dp;

    //Timer
    enum TimerStatus{CONF, RUNNING, TIME_OFF, REST, READY, FINISHED}
    static TimerStatus timer_status = TimerStatus.CONF;
    static long timer_timer = 0;
    private static long timer_start = 0;
    static long timer_start_time_off = 0;
    private static boolean timer_period_ended = false;
    static int timer_period = 0;
    static int timer_period_time = 40;
    static int timer_type_period = 1;//0:up, 1:down
    //Settings
    static boolean screen_on = true;
    static int timer_type = 1;//0:up, 1:down
    static boolean record_player = false;
    static boolean record_pens = false;
    private final static int HELP_VERSION = 4;
    private static int battery_capacity = -1;

    static final MatchData match = new MatchData();
    private Handler handler;
    private ExecutorService executorService;
    private Vibrator vibrator;
    private CommsBT commsBT;
    private BatteryManager batteryManager;

    private static float onTouchStartY = -1;
    private static float onTouchStartX = 0;
    static long draggingEnded;
    private static int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;
    private static boolean hasBTPermission = false;
    private float si_scale_per_pixel = 0;
    private int si_bottom_quarter;
    private int si_below_screen;
    private int confirm_count;
    private long confirm_start_time;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> showSplash);
        super.onCreate(savedInstanceState);
        isScreenRound = getResources().getConfiguration().isScreenRound();
        int heightPixels;
        if(Build.VERSION.SDK_INT >= 30){
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
            widthPixels = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
            widthPixels = displayMetrics.widthPixels;
        }
        SWIPE_THRESHOLD = (int) (widthPixels * .3);
        vh5 = (int) (heightPixels * .05);
        vh10 = heightPixels / 10;
        vh15 = (int) (heightPixels * .15);
        int vh20 = (int) (heightPixels * .2);
        vh25 = (int) (heightPixels * .25);
        vh30 = (int) (heightPixels * .3);
        vh40 = (int) (heightPixels * .4);
        int vh75 = (int) (heightPixels * .75);
        _10dp = getResources().getDimensionPixelSize(R.dimen._10dp);

        int si_item_height = getResources().getDimensionPixelSize(R.dimen.si_item_height);
        si_bottom_quarter = vh75 - si_item_height;
        si_below_screen = heightPixels - si_item_height;
        si_scale_per_pixel = 0.2f / vh25;

        if(Build.VERSION.SDK_INT >= 31){
            vibrator = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
            Log.d(LOG_TAG, "vibrator: " + vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK));
        }else{
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        handler = new Handler(Looper.getMainLooper());
        commsBT = new CommsBT(this);
        batteryManager = (BatteryManager)getSystemService(BATTERY_SERVICE);
        setContentView(R.layout.main);

        // We need to listen for touch on all objects that have a click listener
        int[] ids = new int[]{
                R.id.main, R.id.bConfWatch, R.id.home, R.id.score_home, R.id.away, R.id.score_away
                ,R.id.tTimer, R.id.bPenHome, R.id.bPenAway, R.id.buttons_back, R.id.bOverTimer
                ,R.id.bStart, R.id.bMatchLog, R.id.bBottom, R.id.bConf
                ,R.id.conf, R.id.svConf, R.id.svConfSpinner
                ,R.id.confWatch
                ,R.id.score, R.id.score_player, R.id.score_try, R.id.score_con, R.id.score_goal
                ,R.id.foulPlay, R.id.foul_play, R.id.foulPlay_player, R.id.card_yellow
                ,R.id.penalty_try, R.id.card_red
                ,R.id.extraTime, R.id.extra_time_up, R.id.extra_time_2min, R.id.extra_time_5min
                ,R.id.extra_time_10min
                ,R.id.matchLog, R.id.svMatchLog
                ,R.id.report, R.id.svReport
                ,R.id.correct, R.id.svCorrect
                ,R.id.help, R.id.svHelp
        };
        for(int id : ids){
            findViewById(id).setOnTouchListener(this::onTouch);
        }
        findViewById(R.id.main).setOnClickListener(v -> onMainClick());

        battery = findViewById(R.id.battery);
        time = findViewById(R.id.time);
        home = findViewById(R.id.home);
        home.setOnClickListener(v -> homeClick());
        away = findViewById(R.id.away);
        away.setOnClickListener(v -> awayClick());
        score_home = findViewById(R.id.score_home);
        score_away = findViewById(R.id.score_away);
        sinbins_home = findViewById(R.id.sinbins_home);
        sinbins_away = findViewById(R.id.sinbins_away);
        tTimer = findViewById(R.id.tTimer);
        tTimer.setOnClickListener(v -> timerClick());
        buttons_back = findViewById(R.id.buttons_back);
        bOverTimer = findViewById(R.id.bOverTimer);
        bOverTimer.setOnClickListener(v -> bOverTimerClick());
        bStart = findViewById(R.id.bStart);
        bStart.setOnClickListener(v -> bOverTimerClick());
        bBottom = findViewById(R.id.bBottom);
        bBottom.setOnClickListener(v -> bBottomClick());
        conf = findViewById(R.id.conf);
        conf.onCreateMain(this);
        conf.confSpinner.onCreateMain(this);
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v -> conf.show(this));
        confWatch = findViewById(R.id.confWatch);
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v -> confWatch.show(this));
        confirm = findViewById(R.id.confirm);
        confirm_text = findViewById(R.id.confirm_text);
        findViewById(R.id.confirm_cancel).setOnClickListener(v -> confirm_cancel());

        score = findViewById(R.id.score);
        score.onCreateMain(this);
        foulPlay = findViewById(R.id.foulPlay);
        foulPlay.onCreateMain(this);
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v -> correctClicked());
        correct.onCreateMain(this);
        report = findViewById(R.id.report);
        matchLog = findViewById(R.id.matchLog);
        matchLog.onCreateMain(this);
        bMatchLog = findViewById(R.id.bMatchLog);
        bMatchLog.setOnClickListener(v -> matchLog.show(this, report));
        help = findViewById(R.id.help);
        pen_label = findViewById(R.id.pen_label);
        bPenHome = findViewById(R.id.bPenHome);
        bPenHome.setOnClickListener(v -> bPenHomeClick());
        bPenAway = findViewById(R.id.bPenAway);
        bPenAway.setOnClickListener(v -> bPenAwayClick());
        extraTime = findViewById(R.id.extraTime);
        extraTime.onCreateMain(this);

        //Resize elements for the heightPixels
        battery.setHeight(vh10);
        time.setHeight(vh15);
        score_home.setHeight(vh25);
        score_away.setHeight(vh25);
        tTimer.setHeight(vh30);
        bOverTimer.setHeight(vh25);
        bOverTimer.setPadding(0, vh5, 0, vh5);
        findViewById(R.id.bStartMatchLog).getLayoutParams().height = vh25;
        bStart.setHeight(vh25);
        bStart.setPadding(0, vh5, 0, vh5);
        bMatchLog.setPadding(0, vh5, 0, vh5);
        bBottom.setHeight(vh25);
        bBottom.setPadding(0, vh5, 0, vh5);
        bConf.getLayoutParams().height = vh25;
        ConstraintLayout.LayoutParams pen_label_layoutParams = (ConstraintLayout.LayoutParams) pen_label.getLayoutParams();
        pen_label_layoutParams.height = vh10;
        pen_label_layoutParams.bottomMargin = vh5;
        bPenHome.setHeight(vh20);
        bPenAway.setHeight(vh20);
        if(getResources().getConfiguration().fontScale > 1.1){
            battery.setIncludeFontPadding(false);
            time.setIncludeFontPadding(false);
            score_home.setIncludeFontPadding(false);
            score_away.setIncludeFontPadding(false);
            tTimer.setIncludeFontPadding(false);
            bStart.setIncludeFontPadding(false);
        }

        if(isScreenRound){
            pen_label.getViewTreeObserver().addOnGlobalLayoutListener(()-> {
                if(isPenPadInitialized) return;
                int pad_inner = (pen_label.getWidth()+20)/2;
                int pad_outer = widthPixels/3;
                bPenHome.setPadding(pad_outer, 0, pad_inner, 0);
                bPenAway.setPadding(pad_inner, 0, pad_outer, 0);
                isPenPadInitialized = true;
            });
        }

        if(timer_status == TimerStatus.CONF){
            checkPermissions();
            runInBackground(()-> FileStore.readCustomMatchTypes(this));
            runInBackground(()-> FileStore.readSettings(this));
            runInBackground(()-> FileStore.cleanMatches(this));
        }else{
            updateScore();
        }

        updateBattery();
        update();
        updateSinbins();
        updateButtons();
        updateAfterConfig();
        startBT();
        showSplash = false;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        commsBT.stopBT();
        stopOngoingNotification();
    }

    private void checkPermissions(){
        if(Build.VERSION.SDK_INT >= 33){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
            if(!hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                    || !hasBTPermission
            ){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                        ,Manifest.permission.BLUETOOTH_CONNECT}, 1);
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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
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

    private void runInBackground(Runnable runnable){
        if(executorService == null) executorService = Executors.newCachedThreadPool();
        executorService.execute(runnable);
    }

    void toast(int message){
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            onBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void onBack(){
        if(help.getVisibility() == View.VISIBLE){
            help.setVisibility(View.GONE);
        }else if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(report.getVisibility() == View.VISIBLE){
            report.setVisibility(View.GONE);
        }else if(matchLog.getVisibility() == View.VISIBLE){
            matchLog.setVisibility(View.GONE);
        }else if(extraTime.getVisibility() == View.VISIBLE){
            extraTime.setVisibility(View.GONE);
        }else if(foulPlay.svPlayerNo.getVisibility() == View.VISIBLE){
            foulPlay.svPlayerNo.setVisibility(View.GONE);
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            foulPlay.setVisibility(View.GONE);
        }else if(score.svPlayerNo.getVisibility() == View.VISIBLE){
            score.svPlayerNo.setVisibility(View.GONE);
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(confWatch.getVisibility() == View.VISIBLE){
            confWatch.setVisibility(View.GONE);
            updateAfterConfig();
            runInBackground(() -> FileStore.storeSettings(this));
        }else if(conf.confSpinner.getVisibility() == View.VISIBLE){
            conf.confSpinner.setVisibility(View.GONE);
            conf.requestSVFocus();
        }else if(conf.getVisibility() == View.VISIBLE){
            conf.setVisibility(View.GONE);
            updateAfterConfig();
            runInBackground(() -> FileStore.storeSettings(this));
        }else{
            if(timer_status == TimerStatus.CONF || timer_status == TimerStatus.FINISHED){
                finish();
            }else{
                correct.show(this);
            }
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev){
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
                    draggingEnded = getCurrentTimestamp();
                    onBack();
                    return true;
                }
        }
        return false;
    }
    private void onTouchInit(MotionEvent event){
        onTouchStartY = event.getRawY();
        onTouchStartX = event.getRawX();
        if(help.getVisibility() == View.VISIBLE){
            touchView = help;
        }else if(correct.getVisibility() == View.VISIBLE){
            touchView = correct;
        }else if(report.getVisibility() == View.VISIBLE){
            touchView = report;
        }else if(matchLog.getVisibility() == View.VISIBLE){
            touchView = matchLog;
        }else if(extraTime.getVisibility() == View.VISIBLE){
            touchView = extraTime;
        }else if(foulPlay.svPlayerNo.getVisibility() == View.VISIBLE){
            touchView = foulPlay.svPlayerNo;
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            touchView = foulPlay;
        }else if(score.svPlayerNo.getVisibility() == View.VISIBLE){
            touchView = score.svPlayerNo;
        }else if(score.getVisibility() == View.VISIBLE){
            touchView = score;
        }else if(confWatch.getVisibility() == View.VISIBLE){
            touchView = confWatch;
        }else if(conf.confSpinner.getVisibility() == View.VISIBLE){
            touchView = conf.confSpinner;
        }else if(conf.getVisibility() == View.VISIBLE){
            touchView = conf;
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
    void si_addLayout(ScrollView sv, LinearLayout ll){
        if(!isScreenRound) return;
        ll.setPadding(vh5, vh25, vh5, vh25);
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
                if(ll.getChildCount() > 0 && ll.getChildAt(0).getHeight() > 0){
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    si_scaleItems(ll, 0);
                    sv.setOnScrollChangeListener((v, sx, sy, osx, osy)->si_scaleItems(ll, sy));
                }
            }
        });
    }
    void si_scaleItemsAfterChange(LinearLayout ll, ScrollView sv){
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
                sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                si_scaleItems(ll, sv.getScrollY());
            }
        });
    }
    private void si_scaleItems(LinearLayout ll, int scrollY){
        for(int i = 0; i < ll.getChildCount(); i++){
            View item = ll.getChildAt(i);
            float top = (ll.getY() + item.getY()) - scrollY;
            float scale = 1.0f;
            if(top < 0){
                scale = 0.8f;
            }else if(top < Main.vh25){
                scale = 0.8f + (si_scale_per_pixel * top);
            }else if(top > si_below_screen){
                scale = 0.8f;
            }else if(top > si_bottom_quarter){
                scale = 1.0f - (si_scale_per_pixel * (top - si_bottom_quarter));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }

    private void startBT(){
        if(!hasBTPermission ||
                timer_status == TimerStatus.RUNNING ||
                timer_status == TimerStatus.TIME_OFF ||
                timer_status == TimerStatus.REST ||
                timer_status == TimerStatus.READY) return;
        runInBackground(()-> commsBT.startBT());
    }

    private void timerClick(){
        if(timer_status == TimerStatus.RUNNING){
            singleBeep();
            timer_status = TimerStatus.TIME_OFF;
            timer_start_time_off = getCurrentTimestamp();
            match.logEvent("TIME OFF", null, 0, 0);
            updateButtons();
            handler.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    private void timeOffBuzz(){
        if(timer_status == TimerStatus.TIME_OFF){
            beep();
            handler.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    private void bOverTimerClick(){
        switch(timer_status){
            case CONF:
                match.match_id = getCurrentTimestamp();
                runInBackground(commsBT::stopBT);
            case READY:
                singleBeep();
                timer_status = TimerStatus.RUNNING;
                timer_start = getCurrentTimestamp();
                String kickoffTeam = getKickoffTeam();//capture before increasing timer_period
                timer_period++;
                match.logEvent("START", kickoffTeam, 0, 0);
                updateScore();
                startOngoingNotification();
                break;
            case TIME_OFF:
                //resume running
                singleBeep();
                timer_status = TimerStatus.RUNNING;
                timer_start += (getCurrentTimestamp() - timer_start_time_off);
                match.logEvent("RESUME", null, 0, 0);
                break;
            case REST:
                //get ready for next period
                timer_status = TimerStatus.READY;
                timer_type_period = timer_type;
                break;
            case FINISHED:
                report.show(this, match);
                break;
            default://ignore
                return;
        }
        updateButtons();
    }
    private void bBottomClick(){
        switch(timer_status){
            case TIME_OFF:
                confirm_start();
                break;
            case REST:
                timer_status = TimerStatus.FINISHED;
                timer_period_time = match.period_time;
                timer_type_period = timer_type;
                updateScore();

                runInBackground(() -> FileStore.storeMatch(this));
                startBT();
                stopOngoingNotification();
                updateButtons();
                break;
            case FINISHED:
                timer_status = TimerStatus.CONF;
                timer_timer = 0;
                timer_start = 0;
                timer_start_time_off = 0;
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
                buttons_back.setVisibility(View.GONE);
        }
        updateTimer();
    }
    private void confirm_start(){
        confirm_start_time = getCurrentTimestamp();
        confirm_count = 11;
        confirm_update();
        confirm.setVisibility(View.VISIBLE);
    }
    private void confirm_cancel(){
        confirm_count = -1;
        confirm.setVisibility(View.GONE);
    }
    private void confirm_update(){
        if(confirm_count == -1) return;
        confirm_count--;
        if(confirm_count > 0){
            confirm_text.setText(getString(R.string.confirm_text).replace("10", String.valueOf(confirm_count)));
            handler.postDelayed(this::confirm_update, 1000);
            return;
        }
        //How did someone get here with no events in the match?
        if(!match.events.isEmpty() && match.events.get(match.events.size()-1).what.equals("TIME OFF")){
            match.events.remove(match.events.size()-1);
        }
        match.logEvent("END", null, 0, confirm_start_time);

        timer_status = TimerStatus.REST;
        timer_start = confirm_start_time;
        timer_period_ended = false;
        timer_type_period = 0;
        tTimer.setTextColor(getResources().getColor(R.color.white, getTheme()));

        match.home.sinbins.forEach(sb-> sb.end -= timer_timer);
        match.away.sinbins.forEach(sb-> sb.end -= timer_timer);

        String kickoffTeam = getKickoffTeam();
        if(kickoffTeam != null){
            if(kickoffTeam.equals("home")){
                kickoffTeam = match.home.tot + " " + getString(R.string.kick);
                score_home.setText(kickoffTeam);
            }else{
                kickoffTeam = match.away.tot + " " + getString(R.string.kick);
                score_away.setText(kickoffTeam);
            }
        }
        updateButtons();
        confirm.setVisibility(View.GONE);
    }
    private void update(){
        long milli_secs = updateTime();
        switch(timer_status){
            case RUNNING:
                updateSinbins();
            case REST:
                updateTimer();
                break;
        }
        handler.postDelayed(this::update, 1000 - milli_secs);
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
            findViewById(R.id.pen).setVisibility(View.VISIBLE);
            findViewById(R.id.pen_label).setVisibility(View.VISIBLE);
            tTimer.setHeight(vh30);
            ((ConstraintLayout.LayoutParams) tTimer.getLayoutParams()).bottomMargin = 0;
        }else{
            findViewById(R.id.pen).setVisibility(View.GONE);
            findViewById(R.id.pen_label).setVisibility(View.GONE);
            tTimer.setHeight(vh40);
            ((ConstraintLayout.LayoutParams) tTimer.getLayoutParams()).bottomMargin = vh10;
        }
        score.update();
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
    int getColorFG(String name){
        return switch(name){
            case "gold", "green", "orange", "pink", "white" ->
                    getResources().getColor(R.color.black, null);
            default -> //black blue brown purple red
                    getResources().getColor(R.color.white, null);
        };
    }
    private long updateTime(){
        Date date = new Date();
        long milli_secs = date.getTime() % 1000;
        time.setText(prettyTime(date));
        return milli_secs;
    }
    static String prettyTime(long timestamp){
        Date date = new Date(timestamp);
        return prettyTime(date);
    }
    private static String prettyTime(Date date){
        String strDateFormat = "HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat, Locale.ENGLISH);
        return sdf.format(date);
    }
    private void updateTimer(){
        long milli_secs = 0;
        if(timer_status == TimerStatus.RUNNING || timer_status == TimerStatus.REST){
            milli_secs = getCurrentTimestamp() - timer_start;
        }
        if(timer_status == TimerStatus.TIME_OFF){
            milli_secs = timer_start_time_off - timer_start;
        }
        timer_timer = milli_secs;

        String temp = "";
        if(timer_type_period == 1){
            milli_secs = ((long)timer_period_time * 60000) - milli_secs;
        }
        if(milli_secs < 0){
            milli_secs -= milli_secs * 2;
            temp = "-";
        }
        temp += prettyTimer(milli_secs);
        tTimer.setText(temp);

        if(!timer_period_ended && timer_status == TimerStatus.RUNNING && timer_timer > (long)timer_period_time * 60000){
            timer_period_ended = true;
            tTimer.setTextColor(getResources().getColor(R.color.red, getTheme()));
            beep();
        }
    }
    static String prettyTimer(long milli_secs){
        long tmp = milli_secs % 1000;
        long secs = (milli_secs - tmp) / 1000;
        tmp = secs % 60;
        long minutes = (secs - tmp) / 60;

        String pretty = Long.toString(tmp);
        if(tmp < 10){pretty = "0" + pretty;}
        pretty = minutes + ":" + pretty;

        return pretty;
    }
    private void updateSinbins(){
        getSinbins(match.home, al_sinbins_ui_home, sinbins_home);
        getSinbins(match.away, al_sinbins_ui_away, sinbins_away);
    }
    private final ArrayList<Sinbin> al_sinbins_ui_home = new ArrayList<>();
    private final ArrayList<Sinbin> al_sinbins_ui_away = new ArrayList<>();
    private void getSinbins(MatchData.team team, ArrayList<Sinbin> al_sinbins_ui, LinearLayout llSinbins){
        for(MatchData.sinbin sinbin_data : team.sinbins){
            boolean exists = false;
            for(Sinbin sinbin_ui : al_sinbins_ui){
                if(sinbin_data.id == sinbin_ui.sinbin.id){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                Sinbin sb = new Sinbin(this, sinbin_data, team.color);
                llSinbins.addView(sb);
                al_sinbins_ui.add(sb);
                if(Objects.equals(team.id, "home")){
                    score_home.setHeight(vh15);
                }else{
                    score_away.setHeight(vh15);
                }
            }
        }
        for(int i = al_sinbins_ui.size(); i > 0; i--){
            Sinbin sinbin_ui = al_sinbins_ui.get(i-1);
            if(!team.hasSinbin(sinbin_ui.sinbin.id) || sinbin_ui.sinbin.hide){
                llSinbins.removeView(sinbin_ui);
                al_sinbins_ui.remove(sinbin_ui);
                if(al_sinbins_ui.isEmpty()){
                    if(Objects.equals(team.id, "home")){
                        score_home.setHeight(vh25);
                    }else{
                        score_away.setHeight(vh25);
                    }
                }
            }else{
                sinbin_ui.update();
            }
        }
    }
    private void updateBattery(){
        runInBackground(() -> battery_capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        if(battery_capacity < 0){
            battery.setText("---%");
            handler.postDelayed(this::updateBattery, 100);
        }else{
            String tmp = battery_capacity + "%";
            battery.setText(tmp);
            handler.postDelayed(this::updateBattery, 10000);
        }
    }
    private void bPenHomeClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.home.pens++;
        updateScore();
        match.logEvent("PENALTY", match.home.id, 0, 0);
    }
    private void bPenAwayClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.away.pens++;
        updateScore();
        match.logEvent("PENALTY", match.away.id, 0, 0);
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
        }else{
            score.team = match.away;
            score.setVisibility(View.VISIBLE);
        }
    }
    void tryClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.tries++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("TRY", score.team.id, score.player_no, 0);
    }
    void conversionClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("CONVERSION", score.team.id, score.player_no, 0);
    }
    void goalClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.goals++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("GOAL", score.team.id, score.player_no, 0);
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
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        foulPlay.onPlayerNoClick(score.player_no);
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    void card_yellowClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        long time = getCurrentTimestamp();
        match.logEvent("YELLOW CARD", score.team.id, foulPlay.player_no, time);
        long end = timer_timer + ((long)match.sinbin*60000);
        end += 1000 - (end % 1000);
        score.team.addSinbin(time, end, score.team.id, foulPlay.player_no);
        updateSinbins();
        foulPlay.setVisibility(View.GONE);
        score.clear();
        score.team.yellow_cards++;
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
    }
    void penalty_tryClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.pen_tries++;
        updateScore();
        foulPlay.setVisibility(View.GONE);
        score.clear();
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player_no, 0);
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
    }
    void card_redClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        match.logEvent("RED CARD", score.team.id, foulPlay.player_no, 0);
        foulPlay.setVisibility(View.GONE);
        score.clear();
        score.team.red_cards++;
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
    }

    private void correctClicked(){
        updateScore();
        updateSinbins();
    }

    static long getCurrentTimestamp(){
        Date d = new Date();
        return d.getTime();
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
                return "home";
            }else{
                return "away";
            }
        }
        if(match.away.kickoff){
            if(timer_period % 2 == 0){
                return "away";
            }else{
                return "home";
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
            timer_period_time = match.period_time;
            match.period_count = settings.getInt("period_count");
            match.sinbin = settings.getInt("sinbin");
            match.points_try = settings.getInt("points_try");
            match.points_con = settings.getInt("points_con");
            match.points_goal = settings.getInt("points_goal");

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
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.incomingSettings Exception: " + e.getMessage());
            toast(R.string.fail_receive_settings);
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
            timer_period_time = match.period_time;
            match.period_count = jsonSettings.getInt("period_count");
            match.sinbin = jsonSettings.getInt("sinbin");
            match.points_try = jsonSettings.getInt("points_try");
            match.points_con = jsonSettings.getInt("points_con");
            match.points_goal = jsonSettings.getInt("points_goal");
            screen_on = jsonSettings.getBoolean("screen_on");
            timer_type = jsonSettings.getInt("timer_type");
            timer_type_period = timer_type;
            record_player = jsonSettings.getBoolean("record_player");
            record_pens = jsonSettings.getBoolean("record_pens");

            if(jsonSettings.has("help_version") && HELP_VERSION != jsonSettings.getInt("help_version")){
                showHelp();
                runInBackground(() -> FileStore.storeSettings(this));
            }
            runOnUiThread(this::updateAfterConfig);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.readSettings Exception: " + e.getMessage());
            toast(R.string.fail_read_settings);
        }
    }
    void noSettings(){//Thread: BG
        runOnUiThread(() -> help.show(true));
    }
    void showHelp(){//Thread: Mixed
        runOnUiThread(() -> help.show(false));
    }

    static JSONObject getSettings(){
        JSONObject ret = new JSONObject();
        try{
            ret.put("home_name", match.home.team);
            ret.put("home_color", match.home.color);
            ret.put("away_name", match.away.team);
            ret.put("away_color", match.away.color);
            ret.put("match_type", match.match_type);
            ret.put("period_time", match.period_time);
            ret.put("period_count", match.period_count);
            ret.put("sinbin", match.sinbin);
            ret.put("points_try", match.points_try);
            ret.put("points_con", match.points_con);
            ret.put("points_goal", match.points_goal);
            ret.put("screen_on", screen_on);
            ret.put("timer_type", timer_type);
            ret.put("record_player", record_player);
            ret.put("record_pens", record_pens);
            ret.put("help_version", HELP_VERSION);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Main.getSettings Exception: " + e.getMessage());
            return null;
        }
        return ret;
    }

    void extraTimeChange(int time){
        if(time == 0){
            bBottom.setText(R.string.count_up);
            timer_type_period = 0;
            timer_period_time = match.period_time;
        }else{
            bBottom.setText(String.format("%s %s", time, getString(R.string.min)));
            timer_type_period = timer_type;
            timer_period_time = time;
        }
        updateTimer();
        extraTime.setVisibility(View.GONE);
    }

    private static final VibrationEffect ve_pattern = VibrationEffect.createWaveform(new long[]{300, 500, 300, 500, 300, 500}, -1);
    private static final VibrationEffect ve_single = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
    void beep(){vibrator.vibrate(ve_pattern);}
    private void singleBeep(){vibrator.vibrate(ve_single);}

    private void startOngoingNotification(){
        if(Build.VERSION.SDK_INT < 30){return;}

        String RRW_Notification = "RRW_Notification";
        int RRW_Notification_ID = 1;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(RRW_Notification, getString(R.string.open_rrw), NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent actionIntent = new Intent(this, Main.class);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(
            this,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
            this
            ,RRW_Notification
        )
        .setSmallIcon(R.drawable.icon_foreground)
		.setDefaults(NotificationCompat.DEFAULT_ALL)
		.setCategory(NotificationCompat.CATEGORY_WORKOUT)
		.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		.addAction(
            R.drawable.icon_foreground, getString(R.string.open_rrw),
            actionPendingIntent
		)
        .setOngoing(true);

        Status ongoingActivityStatus = new Status.Builder()
        .addTemplate(getString(R.string.match_ongoing))
        .build();

        OngoingActivity ongoingActivity = new OngoingActivity.Builder(
            this
            ,RRW_Notification_ID
            ,notificationBuilder
        )
        .setStaticIcon(R.drawable.icon_foreground)
        .setTouchIntent(actionPendingIntent)
        .setStatus(ongoingActivityStatus)
        .build();

        ongoingActivity.apply(this);

        notificationManager.notify(RRW_Notification_ID, notificationBuilder.build());
    }
    private void stopOngoingNotification(){
        if(Build.VERSION.SDK_INT < 30){return;}
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
