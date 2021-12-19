package com.windkracht8.rugbyrefereewatch.wearos;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
//TODO: 2nd conf button
public class MainActivity extends Activity {
    private TextView battery;
    private TextView time;
    private TextView score_home;
    private TextView score_away;
    private LinearLayout sinbins_home;
    private LinearLayout sinbins_away;
    private TextView timersec;
    private TextView timermil;
    private TextView timerstatus;
    private Button overtimerbutton;
    private Button bottombutton;
    private Conf conf;
    private Score score;
    private Correct correct;
    private Report report;

    public static matchdata match;
    public static int heightPixels = 0;

    private static String timer_status = "conf";
    public static long timer_timer = 0;
    private static long timer_start = 0;
    private static long timer_start_timeoff = 0;
    private static boolean timer_periodended = false;
    private static int timer_period = 0;
    public static boolean screen_on = true;
    public static boolean countdown = true;

    Handler mainhandler;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            heightPixels = getWindowManager().getMaximumWindowMetrics().getBounds().height();
        }else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            heightPixels = displayMetrics.heightPixels;
        }
        setContentView(R.layout.activity_main);

        mainhandler = new Handler(Looper.getMainLooper());

        match = new matchdata();
        battery = findViewById(R.id.battery);
        time = findViewById(R.id.time);
        score_home = findViewById(R.id.score_home);
        score_home.setOnClickListener(v -> score_homeClick());
        score_away = findViewById(R.id.score_away);
        score_away.setOnClickListener(v -> score_awayClick());
        sinbins_home = findViewById(R.id.sinbins_home);
        sinbins_away = findViewById(R.id.sinbins_away);
        timersec = findViewById(R.id.timersec);
        timersec.setOnClickListener(v -> timerClick());
        timermil = findViewById(R.id.timermil);
        timermil.setOnClickListener(v -> timerClick());
        timerstatus = findViewById(R.id.timerstatus);
        overtimerbutton = findViewById(R.id.overtimerbutton);
        bottombutton = findViewById(R.id.bottombutton);
        conf = findViewById(R.id.conf);
        score = findViewById(R.id.score);
        TextView score_try = findViewById(R.id.score_try);
        score_try.setOnClickListener(v -> tryClick());
        TextView score_con = findViewById(R.id.score_con);
        score_con.setOnClickListener(v -> conversionClick());
        TextView score_goal = findViewById(R.id.score_goal);
        score_goal.setOnClickListener(v -> goalClick());
        TextView card_yellow = findViewById(R.id.card_yellow);
        card_yellow.setOnClickListener(v -> card_yellowClick());
        TextView card_red = findViewById(R.id.card_red);
        card_red.setOnClickListener(v -> card_redClick());
        correct = findViewById(R.id.correct);
        correct.setOnClickListener(v -> correctClicked());
        report = findViewById(R.id.report);

        Filestore.file_readSettings(getBaseContext());
        Filestore.file_cleanMatches(getBaseContext());

        updateBattery();
        updateTime();
        updateTimer();
        update();
    }
    @Override
    public void onBackPressed(){
        if(conf.getVisibility() == View.VISIBLE){
            conf.save(match);
            updateAfterConfig();
            conf.setVisibility(View.GONE);
            Filestore.file_storeSettings(getBaseContext());
        }else if(score.getVisibility() == View.VISIBLE){
            score.setVisibility(View.GONE);
        }else if(correct.getVisibility() == View.VISIBLE){
            correct.setVisibility(View.GONE);
        }else if(report.getVisibility() == View.VISIBLE){
            report.setVisibility(View.GONE);
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
            timer_status = "timeoff";
            timer_start_timeoff = getCurrentTimestamp();
            match.log_event("Time off");
            update();
            mainhandler.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void timeOffBuzz(){
        if(timer_status.equals("timeoff")){
            beep(getBaseContext());
            mainhandler.postDelayed(this::timeOffBuzz, 15000);
        }
    }
    public void bresumeClick(){
        switch(timer_status){
            case "conf":
                match.matchid = getCurrentTimestamp();
            case "ready":
                timer_status = "running";
                timer_period++;
                timer_start = getCurrentTimestamp();
                match.log_event("Start " + getPeriodName(), getKickoffTeam());
                update();
                updateScore();
                break;
            case "timeoff":
                //resume running
                timer_status = "running";
                timer_start += (getCurrentTimestamp() - timer_start_timeoff);
                match.log_event("Resume time");
                update();
                break;
            case "rest":
                //get ready for next period
                timer_status = "ready";
                updateTimer();
                break;
        }
    }
    public void brestClick(){
        match.log_event("Result " + getPeriodName() + " " + match.home.tot + ":" + match.away.tot);

        timer_status = "rest";
        timer_start = getCurrentTimestamp();
        timer_periodended = false;
        timersec.setTextColor(getResources().getColor(R.color.purewhite, getBaseContext().getTheme()));
        timermil.setTextColor(getResources().getColor(R.color.purewhite, getBaseContext().getTheme()));

        for (matchdata.sinbin sb : match.home.sinbins){
            sb.end = sb.end - timer_timer;
        }
        for (matchdata.sinbin sb : match.away.sinbins){
            sb.end = sb.end - timer_timer;
        }
        update();

        if(match.events.get(match.events.size()-1).what.equals("Time off")){
            match.events.remove(match.events.size()-1);
        }
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
    public void bfinishClick(){
        timer_status = "finished";
        update();
        updateScore();

        if(match.events.get(match.events.size()-1).what.equals("Rest star")){
            match.events.remove(match.events.size()-1);
        }
        Filestore.file_storeMatch(getBaseContext(), match);
    }
    public void bclearClick(){
        timer_status = "conf";
        timer_timer = 0;
        timer_start = 0;
        timer_start_timeoff = 0;
        timer_periodended = false;
        timer_period = 0;
        match.clear();
        match = new matchdata();
        update();
        updateScore();
        updateSinbins();
        updateAfterConfig();
    }
    @SuppressLint("SetTextI18n")
    public void update(){
        String uistatus = "";
        overtimerbutton.setVisibility(View.GONE);
        bottombutton.setVisibility(View.GONE);
        switch(timer_status){
            case "conf":
                bottombutton.setText("conf");
                bottombutton.setOnClickListener(v -> bconfClick());
                bottombutton.setVisibility(View.VISIBLE);
            case "ready":
                overtimerbutton.setText("start");
                overtimerbutton.setOnClickListener(v -> bresumeClick());
                overtimerbutton.setVisibility(View.VISIBLE);
                break;
            case "running":
                updateTimer();
                updateSinbins();
                mainhandler.postDelayed(this::update, 50);
                break;
            case "timeoff":
                overtimerbutton.setText("resume");
                overtimerbutton.setOnClickListener(v -> bresumeClick());
                overtimerbutton.setVisibility(View.VISIBLE);
                bottombutton.setText("rest");
                bottombutton.setOnClickListener(v -> brestClick());
                bottombutton.setVisibility(View.VISIBLE);
                uistatus = "time off";
                break;
            case "rest":
                overtimerbutton.setText("next");
                overtimerbutton.setOnClickListener(v -> bresumeClick());
                overtimerbutton.setVisibility(View.VISIBLE);
                bottombutton.setText("finish");
                bottombutton.setOnClickListener(v -> bfinishClick());
                bottombutton.setVisibility(View.VISIBLE);

                updateTimer();
                uistatus = "rest";
                mainhandler.postDelayed(this::update, 50);
                break;
            case "finished":
                overtimerbutton.setText("report");
                overtimerbutton.setOnClickListener(v -> showReport());
                overtimerbutton.setVisibility(View.VISIBLE);
                bottombutton.setText("clear");
                bottombutton.setOnClickListener(v -> bclearClick());
                bottombutton.setVisibility(View.VISIBLE);
                uistatus = "finished";
                break;
        }
        timerstatus.setText(uistatus);
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
    public void updateTime(){
        time.setText(prettyTime());
        mainhandler.postDelayed(this::updateTime, 100);
    }
    public static String prettyTime(long timestamp){
        Date date = new Date(timestamp);
        return prettyTime(date);
    }
    public static String prettyTime(){
        Date date = new Date();
        return prettyTime(date);
    }
    public static String prettyTime(Date date){
        String strDateFormat = "HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat, Locale.ENGLISH);
        return sdf.format(date);
    }
    public void updateTimer(){
        long millisec = 0;
        if(timer_status.equals("running") || timer_status.equals("rest")){
            millisec = getCurrentTimestamp() - timer_start;
        }
        if(timer_status.equals("timeoff")){
            millisec = timer_start_timeoff - timer_start;
        }
        timer_timer = millisec;

        String temp = "";
        if(countdown && !timer_status.equals("rest")){
            millisec = ((long)match.period_time * 60000) - millisec;
        }
        if(millisec < 0){
            millisec -= millisec * 2;
            temp = "-";
        }
        temp += prettyTimer(millisec);
        timersec.setText(temp);
        temp = "." + (millisec % 1000) / 100;
        timermil.setText(temp);

        if(!timer_periodended && timer_status.equals("running") && timer_timer > (long)match.period_time * 60000){
            timer_periodended = true;
            timersec.setTextColor(getResources().getColor(R.color.red, getBaseContext().getTheme()));
            timermil.setTextColor(getResources().getColor(R.color.red, getBaseContext().getTheme()));
            beep(getBaseContext());
        }
    }
    public static String prettyTimer(long millisec){
        long tmp = millisec % 1000;
        long sec = (millisec - tmp) / 1000;
        tmp = sec % 60;
        long mins = (sec - tmp) / 60;

        String pretty = Long.toString(tmp);
        if(tmp < 10){pretty = "0" + pretty;}
        pretty = mins + ":" + pretty;

        return pretty;
    }
    public void updateSinbins(){
        getSinbins(match.home, al_sinbins_ui_home, sinbins_home);
        getSinbins(match.away, al_sinbins_ui_away, sinbins_away);
    }
    private final ArrayList<Sinbin> al_sinbins_ui_home = new ArrayList<>();
    private final ArrayList<Sinbin> al_sinbins_ui_away = new ArrayList<>();
    public void getSinbins(matchdata.team team, ArrayList<Sinbin> al_sinbins_ui, LinearLayout llsinbins){
        for(matchdata.sinbin sinbin_data : team.sinbins){
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
                llsinbins.addView(sb);
                al_sinbins_ui.add(sb);
            }
        }
        for(int i = al_sinbins_ui.size(); i > 0; i--){
            Sinbin sinbin_ui = al_sinbins_ui.get(i-1);
            if(sinbin_ui.sinbin.hide){
                llsinbins.removeView(sinbin_ui);
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
        mainhandler.postDelayed(this::updateBattery, 10000);
    }
    @SuppressLint("SetTextI18n")
    public void score_homeClick(){
        if(timer_status.equals("conf")){
            if(match.home.kickoff){
                match.home.kickoff = false;
                score_home.setText("0");
            }else{
                match.home.kickoff = true;
                score_home.setText("0 KICK");
            }
            match.away.kickoff = false;
            score_away.setText("0");
        }else{
            score.load(match.home);
            score.setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SetTextI18n")
    public void score_awayClick(){
        if(timer_status.equals("conf")){
            if(match.away.kickoff){
                match.away.kickoff = false;
                score_away.setText("0");
            }else{
                match.away.kickoff = true;
                score_away.setText("0 KICK");
            }
            match.home.kickoff = false;
            score_home.setText("0");
        }else{
            score.load(match.away);
            score.setVisibility(View.VISIBLE);
        }
    }
    public void tryClick(){
        score.team.trys++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.log_event("TRY", score.team.id, score.player_no);
    }
    public void conversionClick(){
        score.team.cons++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.log_event("CONVERSION", score.team.id, score.player_no);
    }
    public void goalClick(){
        score.team.goals++;
        updateScore();
        score.setVisibility(View.GONE);
        score.clear();
        match.log_event("GOAL", score.team.id, score.player_no);
    }
    public void updateScore(){
        match.home.tot = match.home.trys*match.points_try +
                match.home.cons*match.points_con +
                match.home.goals*match.points_goal;
        String tmp = match.home.tot + "";
        score_home.setText(tmp);
        match.away.tot = match.away.trys*match.points_try +
                match.away.cons*match.points_con +
                match.away.goals*match.points_goal;
        tmp = match.away.tot + "";
        score_away.setText(tmp);
    }
    public void card_yellowClick(){
        long time = getCurrentTimestamp();
        match.log_event(time, "YELLOW CARD", score.team.id, score.player_no);
        long end = timer_timer + ((long)match.sinbin*60000);
        end += 1000 - (end % 1000);
        score.team.add_sinbin(time, end);
        updateSinbins();
        score.setVisibility(View.GONE);
        score.clear();
    }

    public void card_redClick(){
        match.log_event("RED CARD", score.team.id, score.player_no);
        score.setVisibility(View.GONE);
        score.clear();
    }

    public void correctShow(){
        correct.load(match);
        correct.setVisibility(View.VISIBLE);
    }
    public void correctClicked(){
        updateScore();
    }

    public void bconfClick(){
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
    //TODO: public void incomingSettings(){}
    static long[] buzzpattern = {300,500,300,500,300,500};
    public static void beep(Context c){
        Log.i("MainActivity", "buzz buzz");
        final Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator = ((VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        }else {
            vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        }
        final VibrationEffect ve = VibrationEffect.createWaveform(buzzpattern, -1);
        vibrator.cancel();
        vibrator.vibrate(ve);
    }
}