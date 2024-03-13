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
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.minTouchSize));
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(10, 10, 10, 10);
        int historyMatchPadding = getResources().getDimensionPixelSize(R.dimen.historyMatchPadding);
        setPadding(historyMatchPadding, historyMatchPadding, historyMatchPadding, historyMatchPadding);

        if(!last) setBackgroundResource(R.drawable.background_underline);
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
    private Timer timer;
    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                timer = new Timer();
                timer.schedule(new TimerTask(){
                    @Override
                    public void run(){
                        longPress();
                    }
                }, 500);
                return true;
            case MotionEvent.ACTION_MOVE:
                float diff_x = x - event.getX();
                float diff_y = y - event.getY();
                if(diff_x > 10 || diff_x < -10 || diff_y > 10 || diff_y < -10){
                    timer.cancel();
                }
                return true;
            case MotionEvent.ACTION_UP:
                timer.cancel();
                if(event.getEventTime() - event.getDownTime() < 500){
                    if(main.tabHistory.selecting){
                        toggleSelect();
                    }else{
                        main.tabReport.loadMatch(main, match);
                        main.tabReportLabelClick();
                        performClick();
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @SuppressWarnings("EmptyMethod")//Because we listen to onTouchEvent, we also need to listen to this
    public boolean performClick(){return super.performClick();}

    private void longPress(){new Handler(Looper.getMainLooper()).post(this::toggleSelect);}

    private void toggleSelect(){
        if(is_selected){
            unselect();
        }else{
            TypedValue value = new TypedValue();
            main.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorAccent, value, true);
            setBackgroundColor(value.data);
            setTextColor(main.getColor(R.color.background));
            is_selected = true;
        }
        main.tabHistory.selectionChanged();
    }

    boolean unselect(){
        boolean ret = is_selected;
        setBackgroundColor(0);
        if(!last) setBackgroundResource(R.drawable.background_underline);
        setTextColor(main.getColor(R.color.text));
        is_selected = false;
        return ret;
    }
}
