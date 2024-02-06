package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
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

public class HistoryMatch extends androidx.appcompat.widget.AppCompatTextView{
    private TabHistory hParent;
    private final Handler handler_message;
    public JSONObject match;
    public boolean is_selected;
    private boolean last;

    public HistoryMatch(Context context){super(context);handler_message=null;}
    public HistoryMatch(Context context, Handler handler_message, JSONObject match, TabHistory hParent, boolean last){
        super(context);
        this.hParent = hParent;
        this.handler_message = handler_message;
        this.match = match;
        this.last = last;
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.minTouchSize));
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(10, 10, 10, 10);
        int historyMatchPadding = getResources().getDimensionPixelSize(R.dimen.historyMatchPadding);
        setPadding(historyMatchPadding, historyMatchPadding, historyMatchPadding, historyMatchPadding);

        if(!last){
            setBackgroundResource(R.drawable.background_underline);
        }
        try{
            Date match_date_d = new Date(match.getLong("matchid"));
            String match_date_s = new SimpleDateFormat("E d MMM HH:mm", Locale.getDefault()).format(match_date_d);
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");
            String name_s = match_date_s + " " + Main.getTeamName(context, home) + " v " + Main.getTeamName(context, away);

            setText(name_s);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "HistoryMatch.construct Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_show_match_history, Toast.LENGTH_SHORT).show();
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
                    if(hParent.selecting){
                        toggleSelect();
                    }else{
                        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_HISTORY_MATCH_CLICK, match));
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
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorAccent, value, true);
            setBackgroundColor(value.data);
            setTextColor(getContext().getColor(R.color.background));
            is_selected = true;
        }
        hParent.selectionChanged();
    }

    public boolean unselect(){
        boolean ret = is_selected;
        setBackgroundColor(0);
        if(!last) setBackgroundResource(R.drawable.background_underline);
        setTextColor(getContext().getColor(R.color.text));
        is_selected = false;
        return ret;
    }
}
