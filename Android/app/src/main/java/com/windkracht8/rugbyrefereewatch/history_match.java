package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class history_match extends LinearLayout{
    history hParent;
    private JSONObject match;
    private TextView tvName;
    public boolean isselected = false;

    public history_match(Context context){super(context);}
    public history_match(Context context, JSONObject match, history hParent, boolean last) {
        super(context);
        this.hParent = hParent;
        this.match = match;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.history_match, this, true);
        }
        tvName = findViewById(R.id.tvName);

        if(last){
            tvName.setBackgroundResource(0);
        }
        try {
            Date dMatchdate = new Date(match.getLong("matchid"));
            String sMatchdate = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(dMatchdate);
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");
            String sName = sMatchdate + " " + MainActivity.getTeamName(home) + " v " + MainActivity.getTeamName(away);

            tvName.setText(sName);
        } catch (Exception e) {
            Log.e("history_match", "history_match: " + e.getMessage());
        }

    }
    private float x, y;
    private Timer timer;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                x = event.getX();
                y = event.getY();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        longpress();
                    }
                }, 500);
                return true;
            case (MotionEvent.ACTION_MOVE):
                float diffx = x - event.getX();
                float diffy = y - event.getY();
                if(diffx > 10 || diffx < -10 || diffy > 10 || diffy < -10){
                    timer.cancel();
                }
                return true;
            case (MotionEvent.ACTION_UP):
                timer.cancel();
                if(event.getEventTime() - event.getDownTime() < 500) {
                    MainActivity.historyMatchClick(match);
                    performClick();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void longpress() {
        MainActivity.mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if(isselected) {
                    tvName.setBackgroundColor(0);
                    isselected = false;
                }else{
                    tvName.setBackgroundColor(R.attr.boxBackgroundColor);
                    isselected = true;
                }
                hParent.selectionChanged();
            }
        });
    }

    public void unselect(){
        tvName.setBackgroundColor(0);
        isselected = false;
    }
}
