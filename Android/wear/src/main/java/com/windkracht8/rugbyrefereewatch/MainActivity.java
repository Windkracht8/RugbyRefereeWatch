package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import androidx.fragment.app.FragmentActivity;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends FragmentActivity{
    public static final String RRW_LOG_TAG = "RugbyRefereeWatch";
    private TextView battery;
    private TextView time;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private TextView tTimer;
    private Button bOverTimer;
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
    private Help help;

    public static MatchData match;
    public static int heightPixels = 0;
    public static int vh7 = 0;
    public static int vh10 = 0;

    private static String timer_status = "conf";
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
    public final static int help_version = 2;

    private Handler handler_main;
    private ExecutorService executorService;
    private static BroadcastReceiver rrwReceiver;

    private static long back_time = 0;
    private static float startY = 0;
    private static float startX = 0;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 1000;

    @SuppressLint("MissingInflatedId")//nested layout XMLs are not found
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
        }
        vh7 = (int) (heightPixels * .07);
        vh10 = heightPixels / 10;

        executorService = Executors.newFixedThreadPool(4);
        setContentView(R.layout.activity_main);

        int[] ids = new int[]{R.id.main,R.id.tTimer,R.id.bOverTimer,R.id.conf,
                R.id.conf_label,R.id.color_home_text,R.id.color_home,R.id.color_away_text,
                R.id.color_away,R.id.match_type_text,R.id.period_time_text,R.id.period_time,
                R.id.period_count_text,R.id.period_count,R.id.sinbin_text,R.id.sinbin,
                R.id.points_try_text,R.id.points_try,R.id.points_con_text,R.id.points_con,
                R.id.points_goal_text,R.id.points_goal,R.id.screen_on_text,R.id.screen_on,
                R.id.timer_type_text,R.id.timer_type,R.id.record_player_text,R.id.record_player,
                R.id.record_pens_text,R.id.record_pens,R.id.bHelp_text,R.id.bHelp,R.id.confWatch,
                R.id.conf_watch_label,R.id.timer_type_cw_text,R.id.timer_type_cw,
                R.id.record_player_cw_text,R.id.record_player_cw,R.id.record_pens_cw_text,
                R.id.record_pens_cw,R.id.screen_on_cw_text,R.id.screen_on_cw,R.id.correct,
                R.id.llCorrect,R.id.score_player,R.id.score_try,R.id.score_con,R.id.score_goal,
                R.id.foul_play,R.id.foulPlay_player,R.id.card_yellow,R.id.penalty_try,
                R.id.card_red};
        for(int id : ids){findViewById(id).setOnTouchListener(this::onTouch);}

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
        bBottom = findViewById(R.id.bBottom);
        bBottom.setOnClickListener(v -> bBottomClick());
        conf = findViewById(R.id.conf);
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v -> conf.show());
        confWatch = findViewById(R.id.confWatch);
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v -> confWatch.show());
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
        help = findViewById(R.id.help);
        findViewById(R.id.bHelp).setOnClickListener(v -> showHelp());
        bPenHome = findViewById(R.id.bPenHome);
        bPenHome.setOnClickListener(v -> bPenHomeClick());
        bPenAway = findViewById(R.id.bPenAway);
        bPenAway.setOnClickListener(v -> bPenAwayClick());

        //Resize elements for the heightPixels
        int vh15 = (int) (heightPixels * .15);
        int vh20 = (int) (heightPixels * .2);
        int vh25 = (int) (heightPixels * .25);
        int vh30 = (int) (heightPixels * .3);
        battery.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh15);
        score_home.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        score_away.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        ((TextView)findViewById(R.id.sinbins_space)).setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        tTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh30);
        bOverTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bOverTimer.setMinimumHeight(vh30);
        bBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bBottom.setMinimumHeight(vh25);
        bConf.getLayoutParams().height = vh20;
        bConfWatch.getLayoutParams().height = vh20;

        handler_main = new Handler(Looper.getMainLooper());
        match = new MatchData();

        rrwReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                if(!intent.hasExtra("intent_type")) return;
                switch(intent.getStringExtra("intent_type")) {
                    case "hideSplash":
                        runOnUiThread(() -> hideSplash());
                        break;
                    case "toast":
                        if(!intent.hasExtra("message")) return;
                        runOnUiThread(() -> Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_SHORT).show());
                        break;
                    case "showHelp":
                        if(!intent.hasExtra("help_version")) return;
                        switch(intent.getIntExtra("help_version", 0)){
                            case help_version:
                                break;
                            case 0:
                                help.showWelcome();
                            default:
                                help.setVisibility(View.VISIBLE);
                                executorService.submit(() -> FileStore.file_storeSettings(context));
                        }
                        break;
                    case "storeCustomMatchTypes":
                        executorService.submit(() -> FileStore.file_storeCustomMatchTypes(context));
                        break;
                    case "onReceivePrepare":
                        updateAfterConfig();
                        break;
                }
            }
        };
        registerReceiver(rrwReceiver, new IntentFilter("com.windkracht8.rugbyrefereewatch"));

        executorService.submit(() -> FileStore.file_readSettings(getBaseContext()));
        executorService.submit(() -> FileStore.file_readCustomMatchTypes(getBaseContext()));
        executorService.submit(() -> FileStore.file_cleanMatches(getBaseContext()));

        updateBattery();
        update();
        updateButtons();
        updateAfterConfig();
        handler_main.postDelayed(this::hideSplash, 1000);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(rrwReceiver);
    }
    @Override
    public void onBackPressed(){
        if(back_time > getCurrentTimestamp() - 500){return;}
        back_time = getCurrentTimestamp();
        if(conf.getVisibility() == View.VISIBLE){
            conf.onBackPressed();
            updateAfterConfig();
            executorService.submit(() -> FileStore.file_storeSettings(getBaseContext()));
        }else if(confWatch.getVisibility() == View.VISIBLE){
            confWatch.onBackPressed();
            updateAfterConfig();
            executorService.submit(() -> FileStore.file_storeSettings(getBaseContext()));
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(foulPlay.getVisibility() == View.VISIBLE){
            foulPlay.setVisibility(View.GONE);
        }else if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(report.getVisibility() == View.VISIBLE){
            report.setVisibility(View.GONE);
        }else if(help.getVisibility() == View.VISIBLE){
            help.setVisibility(View.GONE);
        }else{
            if(timer_status.equals("conf") || timer_status.equals("finished")){
                System.exit(0);
            }else{
                correctShow();
            }
        }
    }

    private boolean onTouch(View v, MotionEvent event){
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                startX = event.getX();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                float diffY = (event.getY() - startY) * event.getYPrecision();
                float diffX = (event.getX() - startX) * event.getXPrecision();
                if(Math.abs(diffX) > Math.abs(diffY)){
                    float velocity = (Math.abs(diffX) / (event.getEventTime() - event.getDownTime())) * 1000;
                    if(Math.abs(diffX) > SWIPE_THRESHOLD && velocity > SWIPE_VELOCITY_THRESHOLD){
                        if(diffX > 0){
                            onBackPressed();
                        }
                        return true;
                    }
                }
                return v.performClick();
        }
        return true;
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
                showReport();
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

                executorService.submit(() -> FileStore.file_storeMatch(getBaseContext(), match));
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
                bOverTimer.setText(R.string.start);
                bOverTimer.setVisibility(View.VISIBLE);
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
                bBottom.setVisibility(View.GONE);
                bConf.setVisibility(View.GONE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            case "time_off":
                bConfWatch.setVisibility(View.VISIBLE);
                bOverTimer.setText(R.string.resume);
                bOverTimer.setVisibility(View.VISIBLE);

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
                bBottom.setText(R.string.finish);
                bBottom.setVisibility(View.VISIBLE);
                extraTime.setVisibility(View.GONE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            case "finished":
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setText(R.string.report);
                bOverTimer.setVisibility(View.VISIBLE);
                bBottom.setText(R.string.clear);
                bBottom.setVisibility(View.VISIBLE);
                bConf.setVisibility(View.GONE);
                extraTime.setVisibility(View.GONE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            default:
                bConfWatch.setVisibility(View.GONE);
                bOverTimer.setVisibility(View.GONE);
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

        findViewById(R.id.pen).setVisibility(MainActivity.record_pens ? View.VISIBLE : View.GONE);
        findViewById(R.id.pen_label).setVisibility(MainActivity.record_pens ? View.VISIBLE : View.GONE);

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
                Sinbin sb = new Sinbin(getBaseContext(), null);
                sb.load(sinbin_data, getColor(team.color));
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
            score.setVisibility(View.VISIBLE);
        }
    }
    public void tryClick(){
        score.team.tries++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("TRY", score.team.id, score.player_no, 0, null);
    }
    public void conversionClick(){
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("CONVERSION", score.team.id, score.player_no, 0, null);
    }
    public void goalClick(){
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
        foulPlay.setPlayer(score.player_no);
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    public void card_yellowClick(){
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
        score.team.pen_tries++;
        updateScore();
        foulPlay.setVisibility(View.GONE);
        score.clear();
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player_no, 0, null);
    }
    public void card_redClick(){
        match.logEvent("RED CARD", score.team.id, foulPlay.player_no, 0, null);
        foulPlay.setVisibility(View.GONE);
        score.clear();
    }

    public void correctShow(){
        correct.load(match);
        correct.setVisibility(View.VISIBLE);
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
    public void showReport(){
        report.load(match);
        report.setVisibility(View.VISIBLE);
    }
    public void showHelp(){
        help.setVisibility(View.VISIBLE);
        conf.setVisibility(View.GONE);
    }
    public static boolean incomingSettings(Context context, JSONObject settings_new){
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
            Log.e(MainActivity.RRW_LOG_TAG, "MainActivity.incomingSettings Exception: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with incoming settings");
            return false;
        }
        return true;
    }

    public static void readSettings(Context context, JSONObject jsonSettings){
        if(!timer_status.equals("conf")) return;
        try{
            record_player = jsonSettings.getBoolean("record_player");
            if(jsonSettings.has("record_pens")) record_pens = jsonSettings.getBoolean("record_pens");
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
            Log.e(MainActivity.RRW_LOG_TAG, "MainActivity.readSettings Exception: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with reading settings");
        }
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "hideSplash");
        context.sendBroadcast(intent);
    }

    public static JSONObject getSettings(Context context){
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
            Log.e(MainActivity.RRW_LOG_TAG, "MainActivity.getSettings Exception: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with sending settings");
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
    public static void makeToast(Context context, String message){
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "toast");
        intent.putExtra("message", message);
        context.sendBroadcast(intent);
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
            Log.e(MainActivity.RRW_LOG_TAG, "MainActivity.getTimer Exception: " + e.getMessage());
            return null;
        }
        return ret;
    }
    public void hideSplash(){findViewById(R.id.splash).setVisibility(View.GONE);}
}