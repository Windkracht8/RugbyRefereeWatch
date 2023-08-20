package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Activity{
    public static final String RRW_LOG_TAG = "RugbyRefereeWatch";
    public static boolean isScreenRound;
    private TextView battery;
    private TextView time;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private TextView tTimer;
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

    public static MatchData match;
    public static int heightPixels = 0;
    public static int widthPixels = 0;
    public static int vh7 = 0;
    public static int vh10 = 0;
    public static int vh25 = 0;

    public static String timer_status = "conf";
    public static long timer_timer = 0;
    private static long timer_start = 0;
    private static long timer_start_time_off = 0;
    private static boolean timer_period_ended = false;
    public static int timer_period_time = 40;
    public static int timer_period = 0;
    public static boolean screen_on = true;
    public static int timer_type = 1;//0:up, 1:down
    public static boolean record_player = false;
    public static boolean record_pens = false;
    public static boolean bluetooth = true;
    public final static int help_version = 4;

    private Handler handler_main;
    private ExecutorService executorService;
    private Comms comms;
    public final static int MESSAGE_HIDE_SPLASH = 1;
    public final static int MESSAGE_TOAST = 2;
    public final static int MESSAGE_SHOW_HELP = 3;
    public final static int MESSAGE_READ_SETTINGS = 4;
    public final static int MESSAGE_CUSTOM_MATCH_TYPE = 5;
    public final static int MESSAGE_PREPARE_RECEIVED = 6;

    private static long back_time = 0;
    private static float onTouchStartY = -1;
    private static float onTouchStartX = 0;
    public static long draggingEnded;
    private View touchView;
    private static int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

    @SuppressLint({"MissingInflatedId"}) //nested layout XMLs are not found
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
            widthPixels = getWindowManager().getMaximumWindowMetrics().getBounds().width();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
            widthPixels = displayMetrics.widthPixels;
            SWIPE_THRESHOLD = (int) (widthPixels *.3);
        }
        vh7 = (int) (heightPixels * .07);
        vh10 = heightPixels / 10;

        executorService = Executors.newFixedThreadPool(4);
        setContentView(R.layout.main);

        // We need to listen for touch on all objects that have a click listener
        int[] ids = new int[]{R.id.main,R.id.bConfWatch,R.id.score_home,R.id.score_away,
                R.id.tTimer,R.id.bPenHome,R.id.bPenAway,
                R.id.bOverTimer,R.id.bStart,R.id.bMatchLog,R.id.bBottom,R.id.bConf,
                R.id.button_background,R.id.extraTime,
                R.id.svConf,R.id.llConfWatch,
                R.id.score, R.id.score_player,R.id.score_try,R.id.score_con,R.id.score_goal,
                R.id.foul_play,R.id.foulPlay_player,R.id.card_yellow,R.id.penalty_try,R.id.card_red,
                R.id.matchLog, R.id.svMatchLog,
                R.id.report,
                R.id.correct,R.id.svCorrect,
                R.id.svHelp, R.id.llHelp
        };
        for(int id : ids){findViewById(id).setOnTouchListener(this::onTouch);}
        findViewById(R.id.main).setOnClickListener(v -> onMainClick());

        battery = findViewById(R.id.battery);
        time = findViewById(R.id.time);
        score_home = findViewById(R.id.score_home);
        score_home.setOnClickListener(v -> score_homeClick());
        score_away = findViewById(R.id.score_away);
        score_away.setOnClickListener(v -> score_awayClick());
        sinbins_home = findViewById(R.id.sinbins_home);
        sinbins_away = findViewById(R.id.sinbins_away);
        tTimer = findViewById(R.id.tTimer);
        tTimer.setOnClickListener(v -> timerClick());
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
        findViewById(R.id.score_try).setOnClickListener(v -> tryClick());
        findViewById(R.id.score_con).setOnClickListener(v -> conversionClick());
        findViewById(R.id.score_goal).setOnClickListener(v -> goalClick());
        findViewById(R.id.foul_play).setOnClickListener(v -> foulPlayClick());
        foulPlay = findViewById(R.id.foulPlay);
        findViewById(R.id.card_yellow).setOnClickListener(v -> card_yellowClick());
        findViewById(R.id.penalty_try).setOnClickListener(v -> penalty_tryClick());
        findViewById(R.id.card_red).setOnClickListener(v -> card_redClick());
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v -> correctClicked());
        report = findViewById(R.id.report);
        matchLog = findViewById(R.id.matchLog);
        bMatchLog = findViewById(R.id.bMatchLog);
        bMatchLog.setOnClickListener(v -> matchLog.show(this, report));
        help = findViewById(R.id.help);
        bPenHome = findViewById(R.id.bPenHome);
        bPenHome.setOnClickListener(v -> bPenHomeClick());
        bPenAway = findViewById(R.id.bPenAway);
        bPenAway.setOnClickListener(v -> bPenAwayClick());

        isScreenRound = getBaseContext().getResources().getConfiguration().isScreenRound();
        if(isScreenRound){
            conf.setBackgroundResource(R.drawable.round_bg);
            confWatch.setBackgroundResource(R.drawable.round_bg);
            score.setBackgroundResource(R.drawable.round_bg);
            foulPlay.setBackgroundResource(R.drawable.round_bg);
            report.setBackgroundResource(R.drawable.round_bg);
            correct.setBackgroundResource(R.drawable.round_bg);
            matchLog.setBackgroundResource(R.drawable.round_bg);
            help.setBackgroundResource(R.drawable.round_bg);
        }

        //Resize elements for the heightPixels
        int vh5 = (int) (heightPixels * .05);
        int vh15 = (int) (heightPixels * .15);
        int vh20 = (int) (heightPixels * .2);
        vh25 = (int) (heightPixels * .25);
        int vh30 = (int) (heightPixels * .3);
        int vw30 = (int) (widthPixels * .3);
        battery.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh15);
        time.measure(0, 0);
        if(time.getMeasuredHeight() > vh15){
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) Math.floor((float) time.getMeasuredHeight()/2));
            time.setHeight(vh15);
        }
        score_home.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        score_away.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        ((TextView)findViewById(R.id.sinbins_space)).setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        tTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh30);
        tTimer.measure(0, 0);
        if(tTimer.getMeasuredHeight() > vh30){
            tTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) Math.floor((float) tTimer.getMeasuredHeight()/2));
            tTimer.setHeight(vh30);
        }
        bOverTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bOverTimer.setMinimumHeight(vh30);
        bBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bBottom.setMinimumHeight(vh25);
        bConf.getLayoutParams().height = vh20;
        bConfWatch.getLayoutParams().height = vh20;
        bStart.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bStart.getLayoutParams().height = tTimer.getLayoutParams().height;
        bStart.getLayoutParams().width = widthPixels-vw30;
        bMatchLog.setPadding(0, vh5, vh5, vh5);
        bMatchLog.getLayoutParams().height = vh20;
        bMatchLog.getLayoutParams().width = vw30;

        findViewById(R.id.iHelpNew).getLayoutParams().height = vh15;

        handler_main = new Handler(Looper.getMainLooper());
        match = new MatchData();

        executorService.submit(() -> FileStore.readSettings(getBaseContext(), handler_message));
        executorService.submit(() -> FileStore.readCustomMatchTypes(getBaseContext(), handler_message));
        executorService.submit(() -> FileStore.cleanMatches(getBaseContext(), handler_message));

        updateBattery();
        update();
        updateButtons();
        updateAfterConfig();
        handler_main.postDelayed(this::hideSplash, 1000);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initBT();
        }else{
            bluetooth = false;
            FileStore.storeSettings(getBaseContext(), handler_message);
        }
    }
    private void initBT(){
        if(!bluetooth || !(timer_status.equals("conf") || timer_status.equals("finished"))) return;
        if(comms == null) comms = new Comms(this, handler_message);
        comms.connect(this);
    }

    public final Handler handler_message = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            switch(msg.what){
                case MESSAGE_HIDE_SPLASH:
                    runOnUiThread(() -> hideSplash());
                    break;
                case MESSAGE_TOAST:
                    if(msg.obj instanceof String){
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), (String) msg.obj, Toast.LENGTH_SHORT).show());
                    }else if(msg.obj instanceof Integer){
                        String msg_str = getBaseContext().getString((Integer) msg.obj);
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), msg_str, Toast.LENGTH_SHORT).show());
                    }
                    break;
                case MESSAGE_SHOW_HELP:
                    switch(msg.arg1){
                        case help_version:
                            break;
                        case 0:
                            help.showWelcome();
                        default:
                            help.show();
                            conf.setVisibility(View.GONE);
                            executorService.submit(() -> FileStore.storeSettings(getBaseContext(), handler_message));
                    }
                    break;
                case MESSAGE_READ_SETTINGS:
                    if(!(msg.obj instanceof JSONObject)) return;
                    readSettings((JSONObject) msg.obj);
                    break;
                case MESSAGE_CUSTOM_MATCH_TYPE:
                    executorService.submit(() -> FileStore.storeCustomMatchTypes(getBaseContext(), handler_message));
                    break;
                case MESSAGE_PREPARE_RECEIVED:
                    updateAfterConfig();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed(){
        if(back_time > getCurrentTimestamp() - 500){return;}
        back_time = getCurrentTimestamp();
        if(conf.getVisibility() == View.VISIBLE){
            conf.setVisibility(View.GONE);
            updateAfterConfig();
            executorService.submit(() -> FileStore.storeSettings(getBaseContext(), handler_message));
            if(bluetooth){
                initBT();
            }else{
                if(comms != null) comms.stop();
            }
        }else if(confWatch.getVisibility() == View.VISIBLE){
            confWatch.setVisibility(View.GONE);
            updateAfterConfig();
            executorService.submit(() -> FileStore.storeSettings(getBaseContext(), handler_message));
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            foulPlay.setVisibility(View.GONE);
        }else if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(matchLog.getVisibility() == View.VISIBLE){
            matchLog.setVisibility(View.GONE);
        }else if(report.getVisibility() == View.VISIBLE){
            report.setVisibility(View.GONE);
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
    public void addOnTouch(View v){
        v.setOnTouchListener(this::onTouch);
    }
    public void onMainClick(){
        //We need to do this to make sure that we can listen for onTouch on main
        Log.i(RRW_LOG_TAG, "onMainClick");
    }
    private boolean onTouch(View ignoredV, MotionEvent event){
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                onTouchInit(event);
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
        setTouchView();
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
    private void setTouchView(){
        if(conf.getVisibility() == View.VISIBLE){
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
        }else if(help.getVisibility() == View.VISIBLE){
            touchView = help;
        }else{
            touchView = null;
        }
    }

    public void timerClick(){
        if(timer_status.equals("running")){
            singleBeep(getBaseContext());
            timer_status = "time_off";
            timer_start_time_off = getCurrentTimestamp();
            match.logEvent("TIME OFF", null, 0, 0, null);
            updateButtons();
            handler_main.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void timeOffBuzz(){
        if(timer_status.equals("time_off")){
            beep(getBaseContext());
            handler_main.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void bOverTimerClick(){
        switch(timer_status){
            case "conf":
                match.match_id = getCurrentTimestamp();
                if(comms != null) comms.stop();
            case "ready":
                singleBeep(getBaseContext());
                timer_status = "running";
                timer_start = getCurrentTimestamp();
                String kickoffTeam = getKickoffTeam();//capture before increasing timer_period
                timer_period++;
                match.logEvent("START", kickoffTeam, 0, 0, null);
                updateScore();
                break;
            case "time_off":
                //resume running
                singleBeep(getBaseContext());
                timer_status = "running";
                timer_start += (getCurrentTimestamp() - timer_start_time_off);
                match.logEvent("RESUME", null, 0, 0, null);
                break;
            case "rest":
                //get ready for next period
                timer_status = "ready";
                timer_type = match.timer_type;
                break;
            case "finished":
                report.show(match);
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
                match.logEvent("END", null, 0, 0, match.home.tot + ":" + match.away.tot);

                timer_status = "rest";
                timer_start = getCurrentTimestamp();
                timer_period_ended = false;
                timer_type = 0;
                tTimer.setTextColor(getResources().getColor(R.color.pure_white, getBaseContext().getTheme()));

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
                timer_type = match.timer_type;
                updateScore();

                executorService.submit(() -> FileStore.storeMatch(getBaseContext(), handler_message, match));
                initBT();
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
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            case "ready":
                bConfWatch.setVisibility(View.GONE);
                bOverTimerText = getBaseContext().getString(R.string.start) + " ";
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
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            case "time_off":
                bConfWatch.setVisibility(View.VISIBLE);
                bOverTimer.setText(R.string.resume);
                bOverTimer.setVisibility(View.VISIBLE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);

                bBottomText = getBaseContext().getString(R.string.end) + " ";
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
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
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
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
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
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            default:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
                bStart.setVisibility(View.GONE);
                bMatchLog.setVisibility(View.GONE);
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                extraTime.setVisibility(View.GONE);
                findViewById(R.id.button_background).setVisibility(View.GONE);
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

        score_home.setBackgroundColor(getColor(match.home.color));
        score_away.setBackgroundColor(getColor(match.away.color));

        if(screen_on){
            //If this is not enough, implement wake_lock: https://developer.android.com/training/scheduling/wakelock
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        findViewById(R.id.pen).setVisibility(Main.record_pens ? View.VISIBLE : View.GONE);
        findViewById(R.id.pen_label).setVisibility(Main.record_pens ? View.VISIBLE : View.GONE);

        score.update(match);
    }
    public int getColor(String name){
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
        if(timer_type == 1){
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
            tTimer.setTextColor(getResources().getColor(R.color.red, getBaseContext().getTheme()));
            beep(getBaseContext());
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
                Sinbin sb = new Sinbin(getBaseContext(), null, this, sinbin_data, getColor(team.color));
                llSinbins.addView(sb);
                al_sinbins_ui.add(sb);
            }
        }
        for(int i = al_sinbins_ui.size(); i > 0; i--){
            Sinbin sinbin_ui = al_sinbins_ui.get(i-1);
            if(!team.hasSinbin(sinbin_ui.sinbin.id) || sinbin_ui.sinbin.hide){
                llSinbins.removeView(sinbin_ui);
                al_sinbins_ui.remove(sinbin_ui);
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
        match.logEvent("PENALTY", match.home.id, 0, 0, null);
    }
    public void bPenAwayClick(){
        if(timer_status.equals("conf")){return;}
        match.away.pens++;
        updateScore();
        match.logEvent("PENALTY", match.away.id, 0, 0, null);
    }
    public void score_homeClick(){
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
    public void score_awayClick(){
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
        match.logEvent("TRY", score.team.id, score.player_no, 0, null);
    }
    public void conversionClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("CONVERSION", score.team.id, score.player_no, 0, null);
    }
    public void goalClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        score.team.goals++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("GOAL", score.team.id, score.player_no, 0, null);
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
        match.logEvent("YELLOW CARD", score.team.id, foulPlay.player_no, time, null);
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
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player_no, 0, null);
    }
    public void card_redClick(){
        if(draggingEnded+100 > getCurrentTimestamp()) return;
        match.logEvent("RED CARD", score.team.id, foulPlay.player_no, 0, null);
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
        return getPeriodName(getBaseContext(), period, match.period_count);
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
    public static boolean incomingSettings(Handler handler_message, JSONObject settings_new){
        if(!timer_status.equals("conf")) return false;
        try{
            match.home.team = settings_new.getString("home_name");
            match.home.color = settings_new.getString("home_color");
            match.away.team = settings_new.getString("away_name");
            match.away.color = settings_new.getString("away_color");
            match.match_type = settings_new.getString("match_type");
            match.period_time = settings_new.getInt("period_time");
            timer_period_time = match.period_time;
            match.period_count = settings_new.getInt("period_count");
            match.sinbin = settings_new.getInt("sinbin");
            match.points_try = settings_new.getInt("points_try");
            match.points_con = settings_new.getInt("points_con");
            match.points_goal = settings_new.getInt("points_goal");

            if(settings_new.has("record_player"))
                record_player = settings_new.getInt("record_player") == 1;
            if(settings_new.has("record_pens"))
                record_pens = settings_new.getInt("record_pens") == 1;
            if(settings_new.has("screen_on"))
                screen_on = settings_new.getInt("screen_on") == 1;
            if(settings_new.has("timer_type")) {
                match.timer_type = settings_new.getInt("timer_type");
                timer_type = match.timer_type;
            }
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "MainActivity.incomingSettings Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_receive_settings));
            return false;
        }
        return true;
    }

    private void readSettings(JSONObject jsonSettings){
        if(!timer_status.equals("conf")) return;
        try{
            record_player = jsonSettings.getBoolean("record_player");
            if(jsonSettings.has("record_pens")) record_pens = jsonSettings.getBoolean("record_pens");
            if(jsonSettings.has("bluetooth")){
                bluetooth = jsonSettings.getBoolean("bluetooth");
            }
            if(bluetooth) initBT();
            screen_on = jsonSettings.getBoolean("screen_on");
            match.timer_type = jsonSettings.getInt("timer_type");
            timer_type = match.timer_type;
            match.match_type = jsonSettings.getString("match_type");
            match.period_time = jsonSettings.getInt("period_time");
            timer_period_time = match.period_time;
            match.period_count = jsonSettings.getInt("period_count");
            match.sinbin = jsonSettings.getInt("sinbin");
            match.points_try = jsonSettings.getInt("points_try");
            match.points_con = jsonSettings.getInt("points_con");
            match.points_goal = jsonSettings.getInt("points_goal");
            if(jsonSettings.has("home_color")) match.home.color = jsonSettings.getString("home_color");
            if(jsonSettings.has("away_color")) match.away.color = jsonSettings.getString("away_color");
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "MainActivity.readSettings Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_settings));
        }
    }

    public static JSONObject getSettings(Handler handler_message){
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
            ret.put("record_player", record_player ? 1 : 0);
            ret.put("record_pens", record_pens ? 1 : 0);
            ret.put("screen_on", screen_on ? 1 : 0);
            ret.put("timer_type", match.timer_type);
            ret.put("help_version", help_version);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "MainActivity.getSettings Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_send_settings));
        }
        return ret;
    }

    public void extraTimeChange(){
        switch(extraTime.getSelectedItemPosition()){
            case 1://2 min
                timer_type = match.timer_type;
                timer_period_time = 2;
                break;
            case 2://5 min
                timer_type = match.timer_type;
                timer_period_time = 5;
                break;
            case 3://10 min
                timer_type = match.timer_type;
                timer_period_time = 10;
                break;
            default://UP
                timer_type = 0;
                timer_period_time = match.period_time;
        }
        updateTimer();
    }

    static final long[] buzz_pattern = {300,500,300,500,300,500};
    public static void beep(Context c){
        final Vibrator vibrator;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            vibrator = ((VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else{
            vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        }
        final VibrationEffect ve = VibrationEffect.createWaveform(buzz_pattern, -1);
        vibrator.cancel();
        vibrator.vibrate(ve);
    }
    public static void singleBeep(Context c){
        final Vibrator vibrator;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            vibrator = ((VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else{
            vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        }
        final VibrationEffect ve = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
        vibrator.cancel();
        vibrator.vibrate(ve);
    }
    public static JSONObject getTimer(){
        JSONObject ret = new JSONObject();
        try{
            ret.put("status", timer_status);
            ret.put("timer", timer_timer);
            ret.put("start", timer_start);
            ret.put("start_time_off", timer_start_time_off);
            ret.put("period_ended", timer_period_ended);
            ret.put("period", timer_period);
            ret.put("period_time", timer_period_time);
            ret.put("type", timer_type);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "MainActivity.getTimer Exception: " + e.getMessage());
            return null;
        }
        return ret;
    }
    public void hideSplash(){findViewById(R.id.splash).setVisibility(View.GONE);}
}