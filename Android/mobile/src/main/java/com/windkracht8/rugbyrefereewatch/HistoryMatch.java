package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class HistoryMatch extends LinearLayout{
    TabHistory hParent;
    public JSONObject match;
    private TextView tvName;
    public boolean is_selected = false;

    public HistoryMatch(Context context){super(context);}
    public HistoryMatch(Context context, JSONObject match, TabHistory hParent, boolean last){
        super(context);
        this.hParent = hParent;
        this.match = match;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_history, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.history_match, this, true);

        tvName = findViewById(R.id.tvName);

        if(last){
            findViewById(R.id.llHistoryMatch).setBackgroundResource(0);
        }
        try{
            Date match_date_d = new Date(match.getLong("matchid"));
            String match_date_s = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(match_date_d);
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");
            String name_s = match_date_s + " " + MainActivity.getTeamName(context, home) + " v " + MainActivity.getTeamName(context, away);

            tvName.setText(name_s);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "HistoryMatch.construct Exception: " + e.getMessage());
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
                        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
                        intent.putExtra("intent_type", "historyMatchClick");
                        intent.putExtra("match", match.toString());
                        getContext().sendBroadcast(intent);
                        performClick();
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @SuppressWarnings("EmptyMethod")
    public boolean performClick(){return super.performClick();}

    private void longPress(){new Handler(Looper.getMainLooper()).post(this::toggleSelect);}

    private void toggleSelect(){
        if(is_selected){
            unselect();
        }else{
            final TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);
            tvName.setBackgroundColor(value.data);
            tvName.setTextColor(getContext().getColor(R.color.background));
            is_selected = true;
        }
        hParent.selectionChanged();
    }

    public boolean unselect(){
        boolean ret = is_selected;
        tvName.setBackgroundColor(0);
        tvName.setTextColor(getContext().getColor(R.color.text));
        is_selected = false;
        return ret;
    }
}
