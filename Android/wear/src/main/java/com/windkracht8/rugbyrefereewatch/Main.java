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
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
    public static final String RRW_LOG_TAG = "RugbyRefereeWatch";
    public static boolean isScreenRound;
    private boolean showSplash = true;
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
    private Button bPenHome;
    private Button bPenAway;
    private Button bBottom;
    private ImageButton bConf;
    private Spinner extraTime;
    private ImageButton bConfWatch;
    private Conf conf;
    private ConfWatch confWatch;
    private Score score;
    private FoulPlay foulPlay;
    private Correct correct;
    private Report report;
    private MatchLog matchLog;
    private Help help;
    public CommsBTLog commsBTLog;
    private View touchView;

    public static int heightPixels = 0;
    public static int widthPixels = 0;
    public static int vh10 = 0;
    public static int vh15 = 0;
    public static int vh20 = 0;
    public static int vh25 = 0;
    public static int vh30 = 0;
    public static int vh40 = 0;

    //Timer
    public static String timer_status = "conf";
    public static long timer_timer = 0;
    private static long timer_start = 0;
    private static long timer_start_time_off = 0;
    private static boolean timer_period_ended = false;
    public static int timer_period = 0;
    public static int timer_period_time = 40;
    public static int timer_type_period = 1;//0:up, 1:down
    //Settings
    public static boolean screen_on = true;
    public static int timer_type = 1;//0:up, 1:down
    public static boolean record_player = false;
    public static boolean record_pens = false;
    public static boolean bluetooth = true;
    public final static int help_version = 4;

    public static MatchData match;
    private Handler handler_main;
    private ExecutorService executorService;
    static Vibrator vibrator;
    private CommsBT commsBT;

    public final static int MESSAGE_TOAST = 101;
    public final static int MESSAGE_SHOW_HELP = 102;
    public final static int MESSAGE_SHOW_COMMS_LOG = 103;
    public final static int MESSAGE_READ_SETTINGS = 201;
    public final static int MESSAGE_NO_SETTINGS = 202;
    public final static int MESSAGE_PREPARE_RECEIVED = 301;
    public final static int MESSAGE_STORE_MATCH_TYPES = 302;

    private static float onTouchStartY = -1;
    private static float onTouchStartX = 0;
    public static long draggingEnded;
    private static int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;
    private static boolean hasBTPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> showSplash);
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
        SWIPE_THRESHOLD = (int) (widthPixels * .3);

        if(Build.VERSION.SDK_INT >= 31){
            vibrator = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else{
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        executorService = Executors.newFixedThreadPool(4);
        handler_main = new Handler(Looper.getMainLooper());
        setContentView(R.layout.main);

        // We need to listen for touch on all objects that have a click listener
        int[] ids = new int[]{
                R.id.main, R.id.bConfWatch, R.id.home, R.id.away, R.id.score_home, R.id.score_away,
                R.id.tTimer, R.id.buttons_back, R.id.bPenHome, R.id.bPenAway, R.id.bOverTimer,
                R.id.bStart, R.id.bMatchLog, R.id.bBottom, R.id.bConf, R.id.extraTime,
                R.id.svConf, R.id.svConfSpinner, R.id.svConfWatch,
                R.id.score_player, R.id.score_try, R.id.score_con, R.id.score_goal,
                R.id.foul_play, R.id.foulPlay_player, R.id.card_yellow, R.id.penalty_try, R.id.card_red,
                R.id.svMatchLog, R.id.svReport, R.id.svCorrect,
                R.id.svHelp, R.id.svCommsBTLog
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
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v -> conf.show(this));
        confWatch = findViewById(R.id.confWatch);
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v -> confWatch.show(this));
        extraTime = findViewById(R.id.extraTime);
        extraTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                extraTimeChange();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){}
        });

        score = findViewById(R.id.score);
        score.onCreateMain(this);
        foulPlay = findViewById(R.id.foulPlay);
        foulPlay.onCreateMain(this);
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v -> correctClicked());
        report = findViewById(R.id.report);
        matchLog = findViewById(R.id.matchLog);
        bMatchLog = findViewById(R.id.bMatchLog);
        bMatchLog.setOnClickListener(v -> matchLog.show(this, report));
        help = findViewById(R.id.help);
        commsBTLog = findViewById(R.id.commsBTLog);
        bPenHome = findViewById(R.id.bPenHome);
        bPenHome.setOnClickListener(v -> bPenHomeClick());
        bPenAway = findViewById(R.id.bPenAway);
        bPenAway.setOnClickListener(v -> bPenAwayClick());

        //Resize elements for the heightPixels
        int vh5 = (int) (heightPixels * .05);
        vh10 = heightPixels / 10;
        vh15 = (int) (heightPixels * .15);
        vh20 = (int) (heightPixels * .2);
        vh25 = (int) (heightPixels * .25);
        vh30 = (int) (heightPixels * .3);
        vh40 = (int) (heightPixels * .4);
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
        ConstraintLayout.LayoutParams pen_label_layoutParams = (ConstraintLayout.LayoutParams) findViewById(R.id.pen_label).getLayoutParams();
        pen_label_layoutParams.height = vh10;
        pen_label_layoutParams.bottomMargin = vh5;
        bPenHome.setHeight(vh20);
        bPenAway.setHeight(vh20);

        match = new MatchData();
        executorService.submit(() -> FileStore.readCustomMatchTypes(this));
        executorService.submit(() -> FileStore.readSettings(this));
        executorService.submit(() -> FileStore.cleanMatches(this));

        updateBattery();
        update();
        updateButtons();
        updateAfterConfig();
        showSplash = false;

        executorService.submit(() -> requestPermissions(false));
    }
    public void bluetoothEnabled(){
        executorService.submit(() -> requestPermissions(true));
    }
    private void requestPermissions(boolean onlyBluetooth){
        if(Build.VERSION.SDK_INT >= 33){
            if(bluetooth && onlyBluetooth){
                if(!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) ||
                        !hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                ){
                    ActivityCompat.requestPermissions(this, new String[]{
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN}, 1);
                }
            }else if(bluetooth){
                if(!hasPermission(android.Manifest.permission.POST_NOTIFICATIONS) ||
                        !hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) ||
                        !hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                ){
                    ActivityCompat.requestPermissions(this, new String[]{
                            android.Manifest.permission.POST_NOTIFICATIONS,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN}, 1);
                }
            }else if(!onlyBluetooth){
                if(!hasPermission(Manifest.permission.POST_NOTIFICATIONS)){
                    ActivityCompat.requestPermissions(this, new String[]{
                            android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
            }
        }else if(bluetooth && Build.VERSION.SDK_INT >= 31){
            hasBTPermission = hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT);
            if(!hasBTPermission){
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN}, 1);
            }
        }else if(bluetooth){
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
                    permissions[i].equals(Manifest.permission.BLUETOOTH_SCAN) ||
                    permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    hasBTPermission = false;
                    bluetooth = false;
                    FileStore.storeSettings(this);
                    return;
                }else{
                    hasBTPermission = true;
                }
            }
        }
        if(bluetooth && hasBTPermission) initBT();
    }

    public final Handler handler_message = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch(msg.what){
                case MESSAGE_TOAST:
                    if(msg.obj instanceof String){
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), (String) msg.obj, Toast.LENGTH_SHORT).show());
                    }else if(msg.obj instanceof Integer){
                        String msg_str = getString((Integer) msg.obj);
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), msg_str, Toast.LENGTH_SHORT).show());
                    }
                    break;
                case MESSAGE_SHOW_HELP:
                    if(msg.arg1 == help_version) return;
                    help.show();
                    conf.setVisibility(View.GONE);
                    if(msg.arg1 >= 0){
                        storeSettings();
                    }
                    break;
                case MESSAGE_SHOW_COMMS_LOG:
                    commsBTLog.show();
                    break;
                case MESSAGE_NO_SETTINGS:
                    initBT();
                    help.showWelcome();
                    help.show();
                    storeSettings();
                    break;
                case MESSAGE_READ_SETTINGS:
                    if(!(msg.obj instanceof JSONObject)) return;
                    readSettings((JSONObject) msg.obj);
                    break;
                case MESSAGE_STORE_MATCH_TYPES:
                    storeCustomMatchTypes();
                    break;
                case MESSAGE_PREPARE_RECEIVED:
                    updateAfterConfig();
                    break;
            }
        }
    };
    private void storeSettings(){
        executorService.submit(() -> FileStore.storeSettings(this));
    }
    private void storeCustomMatchTypes(){
        executorService.submit(() -> FileStore.storeCustomMatchTypes(this));
    }

    @Override
    public void onBackPressed(){
        if(conf.confSpinner.getVisibility() == View.VISIBLE){
            conf.confSpinner.setVisibility(View.GONE);
            conf.requestSVFocus();
        }else if(commsBTLog.getVisibility() == View.VISIBLE){
            commsBTLog.setVisibility(View.GONE);
        }else if(conf.getVisibility() == View.VISIBLE){
            conf.setVisibility(View.GONE);
            updateAfterConfig();
            executorService.submit(() -> FileStore.storeSettings(this));
            if(bluetooth){
                initBT();
            }else if(commsBT != null){
                commsBT.stopComms();
            }
        }else if(confWatch.getVisibility() == View.VISIBLE){
            confWatch.setVisibility(View.GONE);
            updateAfterConfig();
            executorService.submit(() -> FileStore.storeSettings(this));
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            foulPlay.setVisibility(View.GONE);
        }else if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(report.getVisibility() == View.VISIBLE){
            report.setVisibility(View.GONE);
        }else if(matchLog.getVisibility() == View.VISIBLE){
            matchLog.setVisibility(View.GONE);
        }else if(help.getVisibility() == View.VISIBLE){
            help.setVisibility(View.GONE);
        }else{
            if(timer_status.equals("conf") || timer_status.equals("finished")){
                System.exit(0);
            }else{
                correct.show(this, match);
            }
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev){
        super.dispatchGenericMotionEvent(ev);
        return true; //Just to let Google know we are listening to rotary events
    }

    public void onMainClick(){
        //We need to do this to make sure that we can listen for onTouch on main
        Log.i(RRW_LOG_TAG, "onMainClick");
    }
    public void addOnTouch(View v){
        v.setOnTouchListener(this::onTouch);
    }
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
                        touchView.setBackgroundResource(R.drawable.round_bg);
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
                    onBackPressed();
                    return true;
                }
        }
        return false;
    }
    private void onTouchInit(MotionEvent event){
        onTouchStartY = event.getRawY();
        onTouchStartX = event.getRawX();
        if(conf.confSpinner.getVisibility() == View.VISIBLE){
            touchView = conf.confSpinner;
        }else if(commsBTLog.getVisibility() == View.VISIBLE){
            touchView = commsBTLog;
        }else if(conf.getVisibility() == View.VISIBLE){
            touchView = conf;
        }else if(confWatch.getVisibility() == View.VISIBLE){
            touchView = confWatch;
        }else if(score.getVisibility() == View.VISIBLE){
            touchView = score;
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            touchView = foulPlay;
        }else if(correct.getVisibility() == View.VISIBLE){
            touchView = correct;
        }else if(report.getVisibility() == View.VISIBLE){
            touchView = report;
        }else if(matchLog.getVisibility() == View.VISIBLE){
            touchView = matchLog;
        }else if(help.getVisibility() == View.VISIBLE){
            touchView = help;
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
    public void initBT(){
        if(!bluetooth || !hasBTPermission || !(timer_status.equals("conf") || timer_status.equals("finished"))) return;
        commsBT = new CommsBT(this);
        executorService.submit(() -> commsBT.startComms());
    }

    public void timerClick(){
        if(timer_status.equals("running")){
            singleBeep();
            timer_status = "time_off";
            timer_start_time_off = getCurrentTimestamp();
            match.logEvent("TIME OFF", null, 0, 0);
            updateButtons();
            handler_main.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void timeOffBuzz(){
        if(timer_status.equals("time_off")){
            beep();
            handler_main.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void bOverTimerClick(){
        switch(timer_status){
            case "conf":
                match.match_id = getCurrentTimestamp();
                if(commsBT != null) commsBT.stopComms();
            case "ready":
                singleBeep();
                timer_status = "running";
                timer_start = getCurrentTimestamp();
                String kickoffTeam = getKickoffTeam();//capture before increasing timer_period
                timer_period++;
                match.logEvent("START", kickoffTeam, 0, 0);
                updateScore();
                startOngoingNotification();
                break;
            case "time_off":
                //resume running
                singleBeep();
                timer_status = "running";
                timer_start += (getCurrentTimestamp() - timer_start_time_off);
                match.logEvent("RESUME", null, 0, 0);
                break;
            case "rest":
                //get ready for next period
                timer_status = "ready";
                timer_type_period = timer_type;
                break;
            case "finished":
                report.show(this, match);
                break;
            default://ignore
                return;
        }
        updateButtons();
    }
    public void bBottomClick(){
        switch(timer_status){
            case "time_off":
                //How did someone get here with no events in the match?
                if(match.events.size() > 0 && match.events.get(match.events.size()-1).what.equals("TIME OFF")){
                    match.events.remove(match.events.size()-1);
                }
                match.logEvent("END", null, 0, 0);

                timer_status = "rest";
                timer_start = getCurrentTimestamp();
                timer_period_ended = false;
                timer_type_period = 0;
                tTimer.setTextColor(getResources().getColor(R.color.white, getTheme()));

                for(MatchData.sinbin sb : match.home.sinbins){
                    sb.end = sb.end - timer_timer;
                }
                for(MatchData.sinbin sb : match.away.sinbins){
                    sb.end = sb.end - timer_timer;
                }

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
                break;
            case "rest":
                timer_status = "finished";
                timer_period_time = match.period_time;
                timer_type_period = timer_type;
                updateScore();

                executorService.submit(() -> FileStore.storeMatch(this, match));
                initBT();
                stopOngoingNotification();
                break;
            case "finished":
                timer_status = "conf";
                timer_timer = 0;
                timer_start = 0;
                timer_start_time_off = 0;
                timer_period_ended = false;
                timer_period = 0;
                match.clear();
                updateScore();
                updateAfterConfig();
                updateSinbins();
                break;
            default://ignore
                return;
        }
        updateButtons();
    }
    private void updateButtons(){
        String bOverTimerText;
        String bBottomText;
        switch(timer_status){
            case "conf":
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
                bStart.setVisibility(View.VISIBLE);
                bMatchLog.setVisibility(View.VISIBLE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.VISIBLE);
                extraTime.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case "ready":
                bConfWatch.setVisibility(View.GONE);
                bOverTimerText = getString(R.string.start) + " ";
                if(match.period_count == 2 && timer_period == 1) {
                    bOverTimerText += getString(R.string._2nd_half);
                }else if(timer_period >= match.period_count){
                    extraTime.setVisibility(View.VISIBLE);
                    extraTimeChange();
                    bOverTimerText += getPeriodName(timer_period+1);
                }else{
                    extraTime.setVisibility(View.GONE);
                    bOverTimerText += getPeriodName(timer_period+1);
                }
                bOverTimer.setText(bOverTimerText);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case "time_off":
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
                extraTime.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case "rest":
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
                extraTime.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            case "finished":
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setText(R.string.report);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setText(R.string.clear);
                bBottom.setVisibility(View.VISIBLE);
                bConf.setVisibility(View.GONE);
                extraTime.setVisibility(View.GONE);
                buttons_back.setVisibility(View.VISIBLE);
                break;
            default:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                extraTime.setVisibility(View.GONE);
                buttons_back.setVisibility(View.GONE);
        }
        updateTimer();
    }
    private void update(){
        long milli_secs = updateTime();
        switch(timer_status){
            case "running":
                updateSinbins();
            case "rest":
                updateTimer();
                break;
        }
        handler_main.postDelayed(this::update, 1000 - milli_secs);
    }

    public void updateAfterConfig(){
        updateTimer();

        home.setBackgroundColor(getColorBG(match.home.color));
        score_home.setTextColor(getColorFG(match.home.color));
        away.setBackgroundColor(getColorBG(match.away.color));
        score_away.setTextColor(getColorFG(match.away.color));

        if(screen_on){
            //If this is not enough, implement wake_lock: https://developer.android.com/training/scheduling/wakelock
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
        score.update(match);
    }
    public int getColorBG(String name){
        switch(name){
            case "black":
                return getResources().getColor(R.color.black, null);
            case "blue":
                return getResources().getColor(R.color.blue, null);
            case "brown":
                return getResources().getColor(R.color.brown, null);
            case "gold":
                return getResources().getColor(R.color.gold, null);
            case "green":
                return getResources().getColor(R.color.green, null);
            case "orange":
                return getResources().getColor(R.color.orange, null);
            case "pink":
                return getResources().getColor(R.color.pink, null);
            case "purple":
                return getResources().getColor(R.color.purple, null);
            case "red":
                return getResources().getColor(R.color.red, null);
            case "white":
                return getResources().getColor(R.color.white, null);
        }
        return getResources().getColor(R.color.black, null);
    }
    public int getColorFG(String name){
        switch(name){
            case "gold":
            case "green":
            case "orange":
            case "pink":
            case "white":
                return getResources().getColor(R.color.black, null);
        }
        //black blue brown purple red
        return getResources().getColor(R.color.white, null);
    }
    public long updateTime(){
        Date date = new Date();
        long milli_secs = date.getTime() % 1000;
        time.setText(prettyTime(date));
        return milli_secs;
    }
    public static String prettyTime(long timestamp){
        Date date = new Date(timestamp);
        return prettyTime(date);
    }
    public static String prettyTime(Date date){
        String strDateFormat = "HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat, Locale.ENGLISH);
        return sdf.format(date);
    }
    public void updateTimer(){
        long milli_secs = 0;
        if(timer_status.equals("running") || timer_status.equals("rest")){
            milli_secs = getCurrentTimestamp() - timer_start;
        }
        if(timer_status.equals("time_off")){
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

        if(!timer_period_ended && timer_status.equals("running") && timer_timer > (long)timer_period_time * 60000){
            timer_period_ended = true;
            tTimer.setTextColor(getResources().getColor(R.color.red, getTheme()));
            beep();
        }
    }
    public static String prettyTimer(long milli_secs){
        long tmp = milli_secs % 1000;
        long secs = (milli_secs - tmp) / 1000;
        tmp = secs % 60;
        long minutes = (secs - tmp) / 60;

        String pretty = Long.toString(tmp);
        if(tmp < 10){pretty = "0" + pretty;}
        pretty = minutes + ":" + pretty;

        return pretty;
    }
    public void updateSinbins(){
        getSinbins(match.home, al_sinbins_ui_home, sinbins_home);
        getSinbins(match.away, al_sinbins_ui_away, sinbins_away);
    }
    private final ArrayList<Sinbin> al_sinbins_ui_home = new ArrayList<>();
    private final ArrayList<Sinbin> al_sinbins_ui_away = new ArrayList<>();
    public void getSinbins(MatchData.team team, ArrayList<Sinbin> al_sinbins_ui, LinearLayout llSinbins){
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
                if(al_sinbins_ui.size() == 0){
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
    public void updateBattery(){
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        String tmp = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "%";
        battery.setText(tmp);
        handler_main.postDelayed(this::updateBattery, 10000);
    }
    public void bPenHomeClick(){
        if(timer_status.equals("conf")){return;}
        match.home.pens++;
        updateScore();
        match.logEvent("PENALTY", match.home.id, 0, 0);
    }
    public void bPenAwayClick(){
        if(timer_status.equals("conf")){return;}
        match.away.pens++;
        updateScore();
        match.logEvent("PENALTY", match.away.id, 0, 0);
    }
    public void homeClick(){
        if(timer_status.equals("conf")){
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
            score.load(match.home);
            score.animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
            score.setVisibility(View.VISIBLE);
        }
    }
    public void awayClick(){
        if(timer_status.equals("conf")){
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
            score.load(match.away);
            score.animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
            score.setVisibility(View.VISIBLE);
        }
    }
    public void tryClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.tries++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("TRY", score.team.id, score.player_no, 0);
    }
    public void conversionClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("CONVERSION", score.team.id, score.player_no, 0);
    }
    public void goalClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.goals++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("GOAL", score.team.id, score.player_no, 0);
    }
    public void updateScore(){
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
    public void foulPlayClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        foulPlay.setPlayer(score.player_no);
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    public void card_yellowClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        long time = getCurrentTimestamp();
        match.logEvent("YELLOW CARD", score.team.id, foulPlay.player_no, time);
        long end = timer_timer + ((long)match.sinbin*60000);
        end += 1000 - (end % 1000);
        score.team.addSinbin(time, end);
        updateSinbins();
        foulPlay.setVisibility(View.GONE);
        score.clear();
    }
    public void penalty_tryClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.pen_tries++;
        updateScore();
        foulPlay.setVisibility(View.GONE);
        score.clear();
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player_no, 0);
    }
    public void card_redClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        match.logEvent("RED CARD", score.team.id, foulPlay.player_no, 0);
        foulPlay.setVisibility(View.GONE);
        score.clear();
    }

    public void correctClicked(){
        updateScore();
        updateSinbins();
    }

    public static long getCurrentTimestamp(){
        Date d = new Date();
        return d.getTime();
    }
    public String getPeriodName(int period){
        return getPeriodName(this, period, match.period_count);
    }
    public static String getPeriodName(Context context, int period, int period_count){
        if(period > period_count){
            if(period == period_count+1){
                return context.getString(R.string.extra_time);
            }else{
                return context.getString(R.string.extra_time) + " " + (period - period_count);
            }
        }else if(period_count == 2){
            switch(period){
                case 1:
                    return context.getString(R.string.first_half);
                case 2:
                    return context.getString(R.string.second_half);
            }
        }else{
            switch(period){
                case 1:
                    return context.getString(R.string._1st);
                case 2:
                    return context.getString(R.string._2nd);
                case 3:
                    return context.getString(R.string._3rd);
                case 4:
                    return context.getString(R.string._4th);
            }
            return String.valueOf(period);
        }
        return "";
    }
    public String getKickoffTeam(){
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
    public static boolean incomingSettings(Handler handler_message, JSONObject settings){
        if(!timer_status.equals("conf")) return false;
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
            Log.e(Main.RRW_LOG_TAG, "Main.incomingSettings Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_receive_settings));
            return false;
        }
        return true;
    }

    private void readSettings(JSONObject jsonSettings){
        if(!timer_status.equals("conf")) return;
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
            if(jsonSettings.get("screen_on") instanceof Integer){//DEPRECATED Sep 2023
                screen_on = jsonSettings.getInt("screen_on") == 1;
            }else{
                screen_on = jsonSettings.getBoolean("screen_on");
            }
            timer_type = jsonSettings.getInt("timer_type");
            timer_type_period = timer_type;
            if(jsonSettings.get("record_player") instanceof Integer){//DEPRECATED Sep 2023
                record_player = jsonSettings.getInt("record_player") == 1;
            }else{
                record_player = jsonSettings.getBoolean("record_player");
            }
            if(jsonSettings.get("record_pens") instanceof Integer){//DEPRECATED Sep 2023
                record_pens = jsonSettings.getInt("record_pens") == 1;
            }else{
                record_pens = jsonSettings.getBoolean("record_pens");
            }

            if(jsonSettings.has("bluetooth")) bluetooth = jsonSettings.getBoolean("bluetooth");
            if(bluetooth) initBT();
            updateAfterConfig();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.readSettings Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_settings));
        }
    }

    public static JSONObject getSettings(){
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
            ret.put("help_version", help_version);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Main.getSettings Exception: " + e.getMessage());
            return null;
        }
        return ret;
    }

    public void extraTimeChange(){
        switch(extraTime.getSelectedItemPosition()){
            case 1://2 min
                timer_type_period = timer_type;
                timer_period_time = 2;
                break;
            case 2://5 min
                timer_type_period = timer_type;
                timer_period_time = 5;
                break;
            case 3://10 min
                timer_type_period = timer_type;
                timer_period_time = 10;
                break;
            default://UP
                timer_type_period = 0;
                timer_period_time = match.period_time;
        }
        updateTimer();
    }

    static final VibrationEffect ve_pattern = VibrationEffect.createWaveform(new long[]{300, 500, 300, 500, 300, 500}, -1);
    static final VibrationEffect ve_single = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
    public static void beep(){
        vibrator.cancel();
        vibrator.vibrate(ve_pattern);
    }
    public static void singleBeep(){
        vibrator.cancel();
        vibrator.vibrate(ve_single);
    }

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
            getBaseContext()
            ,RRW_Notification_ID
            ,notificationBuilder
        )
        .setStaticIcon(R.drawable.icon_foreground)
        .setTouchIntent(actionPendingIntent)
        .setStatus(ongoingActivityStatus)
        .build();

        ongoingActivity.apply(getBaseContext());

        notificationManager.notify(RRW_Notification_ID, notificationBuilder.build());
    }
    private void stopOngoingNotification(){
        if(Build.VERSION.SDK_INT < 30){return;}
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
