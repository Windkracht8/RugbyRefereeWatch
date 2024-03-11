package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLog extends ScrollView{
    private final LinearLayout llMatchLogItems;

    private static int itemHeight;
    private static float scalePerPixel = 0;
    private static float bottom_quarter;
    private static float below_screen;

    public MatchLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.match_log, this, true);
        llMatchLogItems = findViewById(R.id.llMatchLogItems);
    }

    void show(Main main, Report report){
        for(int i = llMatchLogItems.getChildCount(); i > 0; i--){
            llMatchLogItems.removeViewAt(i - 1);
        }
        JSONArray matches = FileStore.readMatches(main);
        try{
            for(int i = matches.length() - 1; i >= 0; i--){
                MatchData match = new MatchData(main, matches.getJSONObject(i));
                addNewItem(main, match, report);
            }
        }catch(JSONException e){
            Log.e(Main.RRW_LOG_TAG, "MatchLog.show Exception: " + e.getMessage());
            main.toast(R.string.fail_show_log);
        }

        if(Main.isScreenRound){
            getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(scalePerPixel > 0 || llMatchLogItems.getChildCount() == 0) return;
                itemHeight = llMatchLogItems.getChildAt(0).getHeight();
                bottom_quarter = Main.vh75 - itemHeight;
                below_screen = Main.heightPixels - itemHeight;
                scalePerPixel = 0.2f / Main.vh25;
                scaleItems(0);
                setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
            });
        }

        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(int i = 0; i< llMatchLogItems.getChildCount(); i++){
            View item = llMatchLogItems.getChildAt(i);
            top = Main.dp50 + (item.getY() - scrollY);
            scale = 1.0f;
            if(top < 0){
                //the item is above the screen
                scale = 0.8f;
            }else if(top < Main.vh25){
                //the item is in the top quarter
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                //the item is below the screen
                scale = 0.8f;
            }else if(top > bottom_quarter){
                //the item is in the bottom quarter
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }
    private void addNewItem(Main main, MatchData match, Report report){
        TextView item = new TextView(main, null, 0, R.style.textView_item);
        Date match_date_d = new Date(match.match_id);
        String text = new SimpleDateFormat("E dd MMM HH:mm", Locale.getDefault()).format(match_date_d);

        text += "\n";
        text += match.home.team.equals(match.home.id) ? match.home.color : match.home.team;
        text += " : ";
        text += match.away.team.equals(match.away.id) ? match.away.color : match.away.team;

        text += "\n";
        text += match.home.tot;
        text += " : ";
        text += match.away.tot;

        item.setText(text);
        item.setOnClickListener(v -> report.show(main, match));
        llMatchLogItems.addView(item);
        main.addOnTouch(item);
    }
}