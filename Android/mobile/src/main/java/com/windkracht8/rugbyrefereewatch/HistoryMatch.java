package com.windkracht8.rugbyrefereewatch;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

class HistoryMatch extends androidx.appcompat.widget.AppCompatTextView{
    private final Main main;
    final JSONObject match;
    boolean is_selected = false;
    private final boolean last;

    HistoryMatch(Main main, JSONObject match, boolean last){
        super(main);
        this.main = main;
        this.match = match;
        this.last = last;
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setMinHeight(getResources().getDimensionPixelSize(R.dimen._48dp));
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(10, 10, 10, 10);
        int _10dp = getResources().getDimensionPixelSize(R.dimen._10dp);
        setPadding(_10dp, _10dp, _10dp, _10dp);

        if(!last) setBackgroundResource(R.drawable.bg_underline);
        try{
            Date match_date_d = new Date(match.getLong("matchid"));
            String match_date_s = new SimpleDateFormat("E d MMM HH:mm", Locale.getDefault()).format(match_date_d);
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");
            String name_s = match_date_s + " " + main.getTeamName(home) + " v " + Main.getTeamName(main, away);

            setText(name_s);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "HistoryMatch.construct Exception: " + e.getMessage());
            Toast.makeText(main, R.string.fail_show_match_history, Toast.LENGTH_SHORT).show();
        }
    }
    private float x, y;
    private final Timer timer = new Timer();
    static boolean isLongPress = false;
    @Override
    public boolean onTouchEvent(MotionEvent event){
        main.onTouchEvent(event);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isLongPress = true;
                x = event.getX();
                y = event.getY();
                timer.schedule(new TimerTask(){
                    @Override
                    public void run(){
                        longPress();
                    }
                }, 500);
                break;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(x - event.getX()) > 10 || Math.abs(y - event.getY()) > 10){
                    isLongPress = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                isLongPress = false;
                if(Math.abs(x - event.getX()) < 10 &&
                        Math.abs(y - event.getY()) < 10 &&
                        event.getEventTime() - event.getDownTime() < 500
                ){
                    if(main.tabHistory.selecting){
                        toggleSelect();
                    }else{
                        main.tabReport.loadMatch(main, match);
                        main.tabReportLabelClick();
                        performClick();
                    }
                }
        }
        return true;
    }

    @SuppressWarnings("EmptyMethod")//Because we listen to onTouchEvent, we also need to listen to this
    public boolean performClick(){return super.performClick();}

    private void longPress(){
        if(isLongPress) new Handler(Looper.getMainLooper()).post(this::toggleSelect);
    }

    private void toggleSelect(){
        if(is_selected){
            unselect();
        }else{
            setBackgroundColor(main.getColor(R.color.button));
            setTextColor(main.getColor(R.color.black));
            is_selected = true;
        }
        main.tabHistory.selectionChanged();
    }

    boolean unselect(){
        boolean ret = is_selected;
        setBackgroundColor(main.getColor(R.color.background));
        if(!last) setBackgroundResource(R.drawable.bg_underline);
        setTextColor(main.getColor(R.color.text));
        is_selected = false;
        return ret;
    }
}
