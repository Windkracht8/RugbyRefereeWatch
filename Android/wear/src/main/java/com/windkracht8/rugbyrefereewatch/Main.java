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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Activity{
    static final String LOG_TAG = "RugbyRefereeWatch";
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
    private TextView delay_end_text;
    private LinearLayout kick_clock_confirm;
    private TextView kick_clock_confirm_label;
    private EditText kick_player;
    private ConfWatch confWatch;
    private Score score;
    private FoulPlay foulPlay;
    private ExtraTime extraTime;
    private Correct correct;
    private View touchView;

    private static int widthPixels;
    static int heightPixels;
    static int vh5;
    static int vh10;
    static int vh15;
    static int vh25;
    static int vh75;
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
    //Kick clocks
    private enum KickClockTypes {PK, CON, RESTART}
    private KickClockTypes kickClockType_home;
    private KickClockTypes kickClockType_away;
    private long kick_clock_home_end = -1;
    private long kick_clock_away_end = -1;

    //Settings
    static boolean screen_on = true;
    static int timer_type = 1;//0:up, 1:down
    static boolean record_player = false;
    static boolean record_pens = false;
    static boolean delay_end = true;
    private final static int HELP_VERSION = 5;
    private static int battery_capacity = -1;

    static final MatchData match = new MatchData();
    private Handler handler;
    private static ExecutorService executorService;
    private Vibrator vibrator;
    private CommsBT commsBT;
    private BatteryManager batteryManager;

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
        int vh50 = heightPixels/2;
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
        setContentView(R.layout.main);

        // We need to listen for touch on all objects that have a click listener
        for(int id : new int[]{
                R.id.main, R.id.bConfWatch, R.id.home, R.id.score_home, R.id.away, R.id.score_away
                ,R.id.tTimer, R.id.kickClockHome, R.id.kickClockAway
                ,R.id.bPenHome, R.id.bPenAway, R.id.buttons_back, R.id.bOverTimer
                ,R.id.bStart, R.id.bMatchLog, R.id.bBottom, R.id.bConf
                ,R.id.kick_clock_confirm_no, R.id.kick_clock_confirm_yes
                ,R.id.confWatch
                ,R.id.score, R.id.score_player, R.id.score_try, R.id.score_con, R.id.score_goal
                ,R.id.foulPlay, R.id.foul_play, R.id.foulPlay_player, R.id.card_yellow
                ,R.id.penaltyTry, R.id.card_red
                ,R.id.extraTime, R.id.extra_time_up, R.id.extra_time_2min, R.id.extra_time_5min
                ,R.id.extra_time_10min
                ,R.id.correct, R.id.svCorrect
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
        kick_player = findViewById(R.id.kick_player);
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v->startActivity(new Intent(this, ConfActivity.class)));
        confWatch = findViewById(R.id.confWatch);
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v->confWatch.show(this));
        delay_end_wrapper = findViewById(R.id.delay_end_wrapper);
        delay_end_text = findViewById(R.id.delay_end_text);
        findViewById(R.id.delay_end_cancel).setOnClickListener(v->delay_end_cancel());

        score = findViewById(R.id.score);
        score.onCreateMain(this);
        foulPlay = findViewById(R.id.foulPlay);
        foulPlay.onCreateMain(this);
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
        buttons_back.getLayoutParams().height = vh50;
        bOverTimer.setHeight(vh25);
        bOverTimer.setPadding(0, vh5, 0, vh5);
        bStart.setHeight(vh25);
        bStart.setPadding(0, vh5, 0, vh5);
        bMatchLog.getLayoutParams().height = vh25;
        bMatchLog.setPadding(0, vh5, 0, vh5);
        bBottom.setHeight(vh25);
        bBottom.setPadding(0, vh5, 0, vh5);
        bConf.getLayoutParams().height = vh25;
        findViewById(R.id.kick_clock_confirm_no).getLayoutParams().width = vw50;
        findViewById(R.id.kick_clock_confirm_yes).getLayoutParams().width = vw50;

        if(getResources().getConfiguration().fontScale > 1.1){
            battery.setIncludeFontPadding(false);
            time.setIncludeFontPadding(false);
            score_home.setIncludeFontPadding(false);
            score_away.setIncludeFontPadding(false);
            tTimer.setIncludeFontPadding(false);
            bStart.setIncludeFontPadding(false);
        }

        if(isScreenRound){
            bPenHome.setPadding(vw30, 0, 0, 0);
            bPenAway.setPadding(0, 0, vw30, 0);
        }

        if(timer_status == TimerStatus.CONF){
            checkPermissions();
            runInBackground(()->FileStore.readSettings(this));
            runInBackground(()->FileStore.cleanMatches(this));
        }else{
            updateScore();
        }

        updateBattery.run();
        update.run();
        updateSinbins();
        updateButtons();
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
            timer_status = TimerStatus.TIME_OFF;
            timer_start_time_off = System.currentTimeMillis();
            match.logEvent("TIME OFF", null, 0, 0);
            updateButtons();
            handler.postDelayed(timeOffBuzz, 15000);
        }
    }
    private final Runnable timeOffBuzz = new Runnable(){@Override public void run(){
        if(timer_status == TimerStatus.TIME_OFF){
            beep();
            handler.postDelayed(timeOffBuzz, 15000);
        }
    }};
    private void bOverTimerClick(){
        switch(timer_status){
            case CONF:
                match.match_id = System.currentTimeMillis();
                runInBackground(commsBT::stopBT);
            case READY:
                singleBeep();
                timer_status = TimerStatus.RUNNING;
                timer_start = System.currentTimeMillis();
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
                timer_start += (System.currentTimeMillis() - timer_start_time_off);
                match.logEvent("RESUME", null, 0, 0);
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
        switch(timer_status){
            case TIME_OFF:
                delay_end_start();
                break;
            case REST:
                timer_status = TimerStatus.FINISHED;
                timer_period_time = match.period_time;
                timer_type_period = timer_type;
                updateScore();

                runInBackground(()->FileStore.storeMatch(this));
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
                buttons_back.setVisibility(View.INVISIBLE);
        }
        updateTimer();
    }
    private void kickClockHomeShow(int label_text, int secs){
        ((TextView)findViewById(R.id.tKickClockHomeLabel)).setText(label_text);
        kick_clock_home_end = System.currentTimeMillis() + (secs*1000L);
        if(isScreenRound){
            if(kick_clock_away_end<0){
                tTimer.setPadding(0, 0, vh10, 0);
            }else{
                tTimer.setPadding(0, 0, 0, 0);
            }
        }
        tKickClockHome.setText(String.valueOf(secs));
        kickClockHome.setVisibility(View.VISIBLE);
    }
    private void kickClockAwayShow(int label_text, int secs){
        ((TextView)findViewById(R.id.tKickClockAwayLabel)).setText(label_text);
        kick_clock_away_end = System.currentTimeMillis() + (secs*1000L);
        if(isScreenRound){
            if(kick_clock_home_end<0){
                tTimer.setPadding(vh10, 0, 0, 0);
            }else{
                tTimer.setPadding(0, 0, 0, 0);
            }
        }
        tKickClockAway.setText(String.valueOf(secs));
        kickClockAway.setVisibility(View.VISIBLE);
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
                kickClockHomeClose();
                return;
        }
        kick_player.setText("");
        kick_player.setVisibility(record_player ? View.VISIBLE : View.GONE);
        findViewById(R.id.kick_clock_confirm_no).setOnClickListener(v->{
            kickClockHomeClose();
            kick_clock_confirm.setVisibility(View.GONE);
        });
        findViewById(R.id.kick_clock_confirm_yes).setOnClickListener(v->{
            score.team = match.home;
            int player = 0;
            if(kick_player.getText().length() > 0) player = Integer.parseInt(kick_player.getText().toString());
            switch(kickClockType_home){
                case CON:
                    conversionClick(player);
                    break;
                case PK:
                    goalClick(player);
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
                kickClockAwayClose();
                return;
        }
        kick_player.setText("");
        kick_player.setVisibility(record_player ? View.VISIBLE : View.GONE);
        findViewById(R.id.kick_clock_confirm_no).setOnClickListener(v->{
            kickClockAwayClose();
            kick_clock_confirm.setVisibility(View.GONE);
        });
        findViewById(R.id.kick_clock_confirm_yes).setOnClickListener(v->{
            score.team = match.away;
            int player = 0;
            if(kick_player.getText().length() > 0) player = Integer.parseInt(kick_player.getText().toString());
            switch(kickClockType_away){
                case CON:
                    conversionClick(player);
                    break;
                case PK:
                    goalClick(player);
                    break;
            }
            kick_clock_confirm.setVisibility(View.GONE);
        });
        kick_clock_confirm.setVisibility(View.VISIBLE);
    }
    void kickClockHomeClose(){
        if(kickClockType_home == KickClockTypes.CON && match.clock_restart > 0){
            kickClockType_home = KickClockTypes.RESTART;
            kickClockHomeShow(R.string.restart, match.clock_restart);
            return;
        }
        kick_clock_home_end = -1;
        if(isScreenRound){
            if(kick_clock_away_end>0){
                tTimer.setPadding(vh10, 0, 0, 0);
            }else{
                tTimer.setPadding(0, 0, 0, 0);
            }
        }
        kickClockHome.setVisibility(View.GONE);
    }
    void kickClockAwayClose(){
        if(kickClockType_away == KickClockTypes.CON && match.clock_restart > 0){
            kickClockType_away = KickClockTypes.RESTART;
            kickClockAwayShow(R.string.restart, match.clock_restart);
            return;
        }
        kick_clock_away_end = -1;
        if(isScreenRound){
            if(kick_clock_home_end>0){
                tTimer.setPadding(0, 0, vh10, 0);
            }else{
                tTimer.setPadding(0, 0, 0, 0);
            }
        }
        kickClockAway.setVisibility(View.GONE);
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
    }
    private void delay_end_cancel(){
        delay_end_count = -1;
        delay_end_wrapper.setVisibility(View.GONE);
    }
    private final Runnable delay_end_update = new Runnable(){@Override public void run(){
        if(delay_end_count == -1) return;
        delay_end_count--;
        if(delay_end_count == 0){
            endPeriod();
        }
        delay_end_text.setText(getString(R.string.delay_end_text).replace("10", String.valueOf(delay_end_count)));
        handler.postDelayed(delay_end_update, 1000);
    }};
    private void endPeriod(){
        //How did someone get here with no events in the match?
        if(!match.events.isEmpty() && match.events.get(match.events.size()-1).what.equals("TIME OFF")){
            match.events.remove(match.events.size()-1);
        }
        match.logEvent("END", null, 0, delay_end_start_time);

        timer_status = TimerStatus.REST;
        timer_start = delay_end_start_time;
        timer_period_ended = false;
        timer_type_period = 0;
        tTimer.setTextColor(getResources().getColor(R.color.white, getTheme()));

        match.home.sinbins.forEach(sb->sb.end -= timer_timer);
        match.away.sinbins.forEach(sb->sb.end -= timer_timer);

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
        if(kick_clock_home_end > 0) kickClockHomeClose();
        if(kick_clock_away_end > 0) kickClockAwayClose();
        delay_end_wrapper.setVisibility(View.GONE);
    }
    private final Runnable update = new Runnable(){@Override public void run(){
        switch(timer_status){
            case RUNNING:
                updateSinbins();
            case REST:
                updateTimer();
                break;
        }
        long milli_secs = updateTime();
        handler.postDelayed(update, 1000 - milli_secs);
    }};

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
            findViewById(R.id.bPenHome).setVisibility(View.VISIBLE);
            findViewById(R.id.pen_label).setVisibility(View.VISIBLE);
            findViewById(R.id.bPenAway).setVisibility(View.VISIBLE);
            ((ConstraintLayout.LayoutParams)tTimer.getLayoutParams()).bottomMargin = 0;
            if(isScreenRound){
                kickClockHome.setPadding(vh10, 0, _10dp, vh5);
                kickClockAway.setPadding(_10dp, 0, vh10, vh5);
            }
        }else{
            findViewById(R.id.bPenHome).setVisibility(View.GONE);
            findViewById(R.id.pen_label).setVisibility(View.GONE);
            findViewById(R.id.bPenAway).setVisibility(View.GONE);
            ((ConstraintLayout.LayoutParams)tTimer.getLayoutParams()).bottomMargin = vh10;
            if(isScreenRound){
                kickClockHome.setPadding(vh10, 0, _10dp, vh10);
                kickClockAway.setPadding(_10dp, 0, vh10, vh10);
            }
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
    private int getColorFG(String name){
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
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return sdf.format(date);
    }
    private void updateTimer(){
        long milli_secs = 0;
        if(timer_status == TimerStatus.RUNNING || timer_status == TimerStatus.REST){
            milli_secs = System.currentTimeMillis() - timer_start;
        }
        if(timer_status == TimerStatus.TIME_OFF){
            milli_secs = timer_start_time_off - timer_start;
        }
        timer_timer = milli_secs;

        String temp = "";
        if(timer_type_period == 1){
            milli_secs = ((long)timer_period_time * 60000) - milli_secs;
        }else{
            int add_periods = timer_status == TimerStatus.READY ? timer_period : timer_period-1;
            if(timer_status != TimerStatus.REST && add_periods > 0){
                milli_secs += ((long)add_periods*timer_period_time)*60000;
            }
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

        if(kick_clock_home_end > 0){
            long kick_clock = kick_clock_home_end - System.currentTimeMillis();
            String tmp = Long.toString(kick_clock / 1000);
            tKickClockHome.setText(tmp);
            if(kick_clock < 0) kickClockHomeClose();
        }
        if(kick_clock_away_end > 0){
            long clock = kick_clock_away_end - System.currentTimeMillis();
            String tmp = Long.toString(clock / 1000);
            tKickClockAway.setText(tmp);
            if(clock < 0) kickClockAwayClose();
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
                Sinbin sb = new Sinbin(this, sinbin_data, getColorFG(team.color));
                llSinbins.addView(sb);
                al_sinbins_ui.add(sb);
                if(team.isHome()){
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
                    if(team.isHome()){
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
    private final Runnable updateBattery = new Runnable(){@Override public void run(){
        runInBackground(()->battery_capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        if(battery_capacity < 0){
            battery.setText("---%");
            handler.postDelayed(updateBattery, 100);
        }else{
            String tmp = battery_capacity + "%";
            battery.setText(tmp);
            handler.postDelayed(updateBattery, 10000);
        }
    }};
    private void bPenHomeClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.home.pens++;
        updateScore();
        match.logEvent("PENALTY", MatchData.HOME_ID, 0, 0);
        kickClockType_home = KickClockTypes.PK;
        kickClockHomeShow(R.string.pk, match.clock_pk);
    }
    private void bPenAwayClick(){
        if(timer_status == TimerStatus.CONF){return;}
        match.away.pens++;
        updateScore();
        match.logEvent("PENALTY", MatchData.AWAY_ID, 0, 0);
        kickClockType_away = KickClockTypes.PK;
        kickClockAwayShow(R.string.pk, match.clock_pk);
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
            score.player_clear();
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
            score.player_clear();
            score.setVisibility(View.VISIBLE);
        }
    }
    void tryClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.tries++;
        updateScore();
        match.logEvent("TRY", score.team.id, score.player(), 0);
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
        conversionClick(score.player());
        score.setVisibility(View.GONE);
    }
    private void conversionClick(int player){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.cons++;
        updateScore();
        match.logEvent("CONVERSION", score.team.id, player, 0);
        if(score.team.isHome()){
            kickClockHomeClose();
        }else{
            kickClockAwayClose();
        }
    }
    void goalClick(){
        goalClick(score.player());
        score.setVisibility(View.GONE);
    }
    private void goalClick(int player){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.goals++;
        updateScore();
        match.logEvent("GOAL", score.team.id, player, 0);
        if(score.team.isHome()){
            kickClockHomeClose();
        }else{
            kickClockAwayClose();
        }
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
        foulPlay.player(score.player());
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    void card_yellowClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        long time = System.currentTimeMillis();
        match.logEvent("YELLOW CARD", score.team.id, foulPlay.player(), time);
        long end = timer_timer + ((long)match.sinbin*60000);
        end += 1000 - (end % 1000);
        score.team.addSinbin(time, end, score.team.id, foulPlay.player());
        updateSinbins();
        score.team.yellow_cards++;
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
        foulPlay.setVisibility(View.GONE);
    }
    void penaltyTryClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        score.team.pen_tries++;
        updateScore();
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player(), 0);
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
        foulPlay.setVisibility(View.GONE);
    }
    void card_redClick(){
        if(draggingEnded+100 > System.currentTimeMillis()) return;
        match.logEvent("RED CARD", score.team.id, foulPlay.player(), 0);
        score.team.red_cards++;
        if(record_pens){
            score.team.pens++;
            updateScore();
        }
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
            timer_period_time = match.period_time;
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
            timer_period_time = match.period_time;
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
            ret.put("clock_pk", match.clock_pk);
            ret.put("clock_con", match.clock_con);
            ret.put("clock_restart", match.clock_restart);
            ret.put("screen_on", screen_on);
            ret.put("timer_type", timer_type);
            ret.put("record_player", record_player);
            ret.put("record_pens", record_pens);
            ret.put("delay_end", delay_end);
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
