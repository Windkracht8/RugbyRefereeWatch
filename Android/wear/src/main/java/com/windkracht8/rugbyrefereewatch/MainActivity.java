package com.windkracht8.rugbyrefereewatch;

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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private TextView battery;
    private TextView time;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private TextView tTimer;
    private TextView tTimerStatus;
    private Button bOverTimer;
    private Button bBottom;
    private ImageButton bConf;
    private ImageButton bConfWatch;
    private Conf conf;
    private Score score;
    private FoulPlay foulPlay;
    private Correct correct;
    private Report report;
    private Help help;

    public static MatchData match;
    public static int heightPixels = 0;
    public static int vh5 = 0;
    public static int vh10 = 0;
    public static int vh15 = 0;
    public static int vh25 = 0;
    public static int vh30 = 0;
    public static int vh80 = 0;
    public static int vh90 = 0;

    private static String timer_status = "conf";
    public static long timer_timer = 0;
    private static long timer_start = 0;
    private static long timer_start_time_off = 0;
    private static boolean timer_period_ended = false;
    public static int timer_period = 0;
    public static boolean record_player = false;
    public static boolean screen_on = true;
    public static int timer_type = 1;//0:up, 1:down
    public final static int help_version = 2;

    private Handler handler_main;
    private ExecutorService executorService;
    private static BroadcastReceiver rrwReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        executorService = Executors.newFixedThreadPool(4);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
        }else{
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
        }
        vh5 = heightPixels / 20;
        vh10 = heightPixels / 10;
        vh15 = (int) (heightPixels * .15);
        vh25 = heightPixels / 4;
        vh30 = (int) (heightPixels * .3);
        vh80 = (int) (heightPixels * .8);
        vh90 = (int) (heightPixels * .9);

        setContentView(R.layout.activity_main);
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
        tTimerStatus = findViewById(R.id.tTimerStatus);
        bOverTimer = findViewById(R.id.bOverTimer);
        bBottom = findViewById(R.id.bBottom);
        bConf = findViewById(R.id.bConf);
        bConf.setOnClickListener(v -> bConfClick());
        bConfWatch = findViewById(R.id.bConfWatch);
        bConfWatch.setOnClickListener(v -> bConfWatchClick());
        conf = findViewById(R.id.conf);
        score = findViewById(R.id.score);
        TextView score_try = findViewById(R.id.score_try);
        score_try.setOnClickListener(v -> tryClick());
        TextView score_con = findViewById(R.id.score_con);
        score_con.setOnClickListener(v -> conversionClick());
        findViewById(R.id.score_goal).setOnClickListener(v -> goalClick());
        findViewById(R.id.foul_play).setOnClickListener(v -> foulPlayClick());
        foulPlay = findViewById(R.id.card);
        findViewById(R.id.card_yellow).setOnClickListener(v -> card_yellowClick());
        findViewById(R.id.penalty_try).setOnClickListener(v -> penalty_tryClick());
        findViewById(R.id.card_red).setOnClickListener(v -> card_redClick());
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v -> correctClicked());
        report = findViewById(R.id.report);
        help = findViewById(R.id.help);
        findViewById(R.id.bHelp).setOnClickListener(v -> showHelp());

        //Resize elements for the heightPixels
        battery.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh15);
        score_home.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        score_away.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        findViewById(R.id.sinbin_space).setMinimumHeight(vh15);
        tTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh30);
        tTimerStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh15);
        bOverTimer.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bOverTimer.setMinimumHeight(vh30);
        bBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh10);
        bBottom.setMinimumHeight(vh25);
        bConf.setMaxHeight(vh15);
        bConfWatch.setMaxHeight(vh25);

        handler_main = new Handler(Looper.getMainLooper());
        match = new MatchData();

        rrwReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                if(!intent.hasExtra("intent_type")) return;
                switch(intent.getStringExtra("intent_type")) {
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
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(rrwReceiver);
    }
    @Override
    public void onBackPressed(){
        if(conf.getVisibility() == View.VISIBLE){
            conf.save(match);
            updateAfterConfig();
            conf.setVisibility(View.GONE);
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

    public void timerClick(){
        if(timer_status.equals("running")){
            singleBeep(getBaseContext());
            timer_status = "time_off";
            timer_start_time_off = getCurrentTimestamp();
            match.logEvent("Time off", null, null, 0);
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
    public void bResumeClick(){
        switch(timer_status){
            case "conf":
                match.match_id = getCurrentTimestamp();
            case "ready":
                singleBeep(getBaseContext());
                timer_status = "running";
                timer_start = getCurrentTimestamp();
                String kickoffTeam = getKickoffTeam();//capture before increasing timer_period
                timer_period++;
                match.logEvent("Start " + getPeriodName(), kickoffTeam, null, 0);
                updateScore();
                break;
            case "time_off":
                //resume running
                singleBeep(getBaseContext());
                timer_status = "running";
                timer_start += (getCurrentTimestamp() - timer_start_time_off);
                match.logEvent("Resume time", null, null, 0);
                break;
            case "rest":
                //get ready for next period
                timer_status = "ready";
                break;
        }
        updateButtons();
    }
    public void bRestClick(){
        //How did someone get here with no events in the match?
        if(match.events.size() > 0 && match.events.get(match.events.size()-1).what.equals("Time off")){
            match.events.remove(match.events.size()-1);
        }
        match.logEvent("Result " + getPeriodName() + " " + match.home.tot + ":" + match.away.tot, null, null, 0);

        timer_status = "rest";
        timer_start = getCurrentTimestamp();
        timer_period_ended = false;
        tTimer.setTextColor(getResources().getColor(R.color.pure_white, getBaseContext().getTheme()));

        for(MatchData.sinbin sb : match.home.sinbins){
            sb.end = sb.end - timer_timer;
        }
        for(MatchData.sinbin sb : match.away.sinbins){
            sb.end = sb.end - timer_timer;
        }
        updateButtons();

        String kickoffTeam = getKickoffTeam();
        if(kickoffTeam != null){
            if(kickoffTeam.equals("home")){
                kickoffTeam = match.home.tot + " KICK";
                score_home.setText(kickoffTeam);
            }else{
                kickoffTeam = match.away.tot + " KICK";
                score_away.setText(kickoffTeam);
            }
        }
    }
    public void bFinishClick(){
        timer_status = "finished";
        updateButtons();
        updateScore();

        executorService.submit(() -> FileStore.file_storeMatch(getBaseContext(), match));
    }
    public void bClearClick(){
        timer_status = "conf";
        timer_timer = 0;
        timer_start = 0;
        timer_start_time_off = 0;
        timer_period_ended = false;
        timer_period = 0;
        match.clear();
        updateScore();
        updateButtons();
        updateAfterConfig();
        updateSinbins();
    }
    private void updateButtons(){
        String ui_status = "";
        bOverTimer.setVisibility(View.GONE);
        bOverTimer.setOnClickListener(null);
        bBottom.setVisibility(View.GONE);
        bBottom.setOnClickListener(null);
        bConf.setVisibility(View.GONE);
        bConfWatch.setVisibility(View.GONE);
        findViewById(R.id.button_background).setVisibility(View.GONE);
        switch(timer_status){
            case "conf":
                bConf.setVisibility(View.VISIBLE);
            case "ready":
                bOverTimer.setText(R.string.start);
                bOverTimer.setOnClickListener(v -> bResumeClick());
                bOverTimer.setVisibility(View.VISIBLE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                break;
            case "time_off":
                bOverTimer.setText(R.string.resume);
                bOverTimer.setOnClickListener(v -> bResumeClick());
                bOverTimer.setVisibility(View.VISIBLE);
                bBottom.setText(R.string.rest);
                bBottom.setOnClickListener(v -> bRestClick());
                bBottom.setVisibility(View.VISIBLE);
                bConfWatch.setVisibility(View.VISIBLE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                ui_status = "time off";
                break;
            case "rest":
                bOverTimer.setText(R.string.next);
                bOverTimer.setOnClickListener(v -> bResumeClick());
                bOverTimer.setVisibility(View.VISIBLE);
                bBottom.setText(R.string.finish);
                bBottom.setOnClickListener(v -> bFinishClick());
                bBottom.setVisibility(View.VISIBLE);
                bConfWatch.setVisibility(View.VISIBLE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                ui_status = "rest";
                break;
            case "finished":
                bOverTimer.setText(R.string.report);
                bOverTimer.setOnClickListener(v -> showReport());
                bOverTimer.setVisibility(View.VISIBLE);
                bBottom.setText(R.string.clear);
                bBottom.setOnClickListener(v -> bClearClick());
                bBottom.setVisibility(View.VISIBLE);
                findViewById(R.id.button_background).setVisibility(View.VISIBLE);
                ui_status = "finished";
                break;
        }
        tTimerStatus.setText(ui_status);
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

        score.update(match);
    }
    public int getColor(String name){
        return getResources().getColor(getResources().getIdentifier(name, "color", getPackageName()), getBaseContext().getTheme());
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
        if(timer_type == 1 && !timer_status.equals("rest")){
            milli_secs = ((long)match.period_time * 60000) - milli_secs;
        }
        if(milli_secs < 0){
            milli_secs -= milli_secs * 2;
            temp = "-";
        }
        temp += prettyTimer(milli_secs);
        tTimer.setText(temp);

        if(!timer_period_ended && timer_status.equals("running") && timer_timer > (long)match.period_time * 60000){
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
        match.logEvent("TRY", score.team.id, score.player_no, 0);
    }
    public void conversionClick(){
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.logEvent("CONVERSION", score.team.id, score.player_no, 0);
    }
    public void goalClick(){
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
        String tmp = match.home.tot + "";
        score_home.setText(tmp);
        match.away.tot = match.away.tries*match.points_try +
                match.away.cons*match.points_con +
                match.away.pen_tries*(match.points_try + match.points_con) +
                match.away.goals*match.points_goal;
        tmp = match.away.tot + "";
        score_away.setText(tmp);
    }
    public void foulPlayClick(){
        foulPlay.setPlayer(score.player_no);
        foulPlay.setVisibility(View.VISIBLE);
        score.setVisibility(View.GONE);
    }
    public void card_yellowClick(){
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
        score.team.pen_tries++;
        updateScore();
        foulPlay.setVisibility(View.GONE);
        score.clear();
        match.logEvent("PENALTY TRY", score.team.id, foulPlay.player_no, 0);
    }
    public void card_redClick(){
        match.logEvent("RED CARD", score.team.id, foulPlay.player_no, 0);
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

    public void bConfWatchClick(){
        conf.load(match);
        conf.onlyWatchSettings();
        conf.setVisibility(View.VISIBLE);
    }
    public void bConfClick(){
        conf.load(match);
        conf.setVisibility(View.VISIBLE);
    }

    public static long getCurrentTimestamp(){
        Date d = new Date();
        return d.getTime();
    }
    public String getPeriodName(){
        if(match.period_count == 2){
            switch(timer_period){
                case 1:
                    return "first half";
                case 2:
                    return "second half";
                default:
                    return "extra time";
            }
        }
        return "period " + timer_period;
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
            match.period_count = settings_new.getInt("period_count");
            match.sinbin = settings_new.getInt("sinbin");
            match.points_try = settings_new.getInt("points_try");
            match.points_con = settings_new.getInt("points_con");
            match.points_goal = settings_new.getInt("points_goal");

            if(settings_new.has("record_player"))
                record_player = settings_new.getInt("record_player") == 1;
            if(settings_new.has("screen_on"))
                screen_on = settings_new.getInt("screen_on") == 1;
            if(settings_new.has("timer_type"))
                timer_type = settings_new.getInt("timer_type");
        }catch(Exception e){
            Log.e("MainActivity", "incomingSettings: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with incoming settings");
            return false;
        }
        return true;
    }

    public static void readSettings(Context context, JSONObject jsonSettings){
        if(!timer_status.equals("conf")) return;
        try{
            record_player = jsonSettings.getBoolean("record_player");
            screen_on = jsonSettings.getBoolean("screen_on");
            timer_type = jsonSettings.getInt("timer_type");
            match.match_type = jsonSettings.getString("match_type");
            match.period_time = jsonSettings.getInt("period_time");
            match.period_count = jsonSettings.getInt("period_count");
            match.sinbin = jsonSettings.getInt("sinbin");
            match.points_try = jsonSettings.getInt("points_try");
            match.points_con = jsonSettings.getInt("points_con");
            match.points_goal = jsonSettings.getInt("points_goal");
        }catch(Exception e){
            Log.e("MainActivity", "readSettings: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with reading settings");
        }
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
            ret.put("screen_on", screen_on ? 1 : 0);
            ret.put("timer_type", timer_type);
            ret.put("help_version", help_version);
        }catch(Exception e){
            Log.e("MainActivity", "getSettings: " + e.getMessage());
            MainActivity.makeToast(context, "Problem with sending settings");
        }
        return ret;
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
}