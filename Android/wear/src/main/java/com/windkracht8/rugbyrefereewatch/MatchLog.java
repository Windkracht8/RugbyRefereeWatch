package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

public class MatchLog extends ScrollView{
    private boolean itemHeightInit = false;
    private int itemHeight = 200;
    private int topBottomMargin = 0;
    private float scalePerPixel = 0;
    private LinearLayout matchLogList;

    public MatchLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_log, Toast.LENGTH_SHORT).show();return;}
        inflater.inflate(R.layout.match_log, this, true);
        matchLogList = findViewById(R.id.matchLogList);
    }

    void show(Main main, Report report){
        for(int i = matchLogList.getChildCount(); i > 0; i--){
            matchLogList.removeViewAt(i - 1);
        }
        JSONArray matches = FileStore.readMatches(main);
        try{
            for(int i = matches.length() - 1; i >= 0; i--){
                MatchData match = new MatchData(main, matches.getJSONObject(i));
                MatchLogMatch matchLogMatch = new MatchLogMatch(main, match, report);
                matchLogList.addView(matchLogMatch);
                main.addOnTouch(matchLogMatch);
            }
        }catch(JSONException e){
            Log.e(Main.RRW_LOG_TAG, "MatchLog.show Exception: " + e.getMessage());
            main.toast(R.string.fail_show_log);
        }
        findViewById(R.id.match_log_label).getLayoutParams().height = Main.vh25;
        ((LayoutParams) findViewById(R.id.llMatchLog).getLayoutParams()).bottomMargin = getResources().getDimensionPixelSize(R.dimen.ll_in_sc_padding) + Main.vh25;

        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(!Main.isScreenRound || itemHeightInit) return;
            if(matchLogList.getChildCount() == 0) return;
            itemHeightInit = true;
            itemHeight = matchLogList.getChildAt(0).getHeight();
            topBottomMargin = (Main.heightPixels - itemHeight) / 3;
            scalePerPixel = .5f / (topBottomMargin + itemHeight);
            onScroll(0);
            setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> onScroll(scrollY));
        });
    }
    private void onScroll(int scrollY){
        float top;
        float bottom;
        float scale;
        for(int i=0; i<matchLogList.getChildCount(); i++){
            View item = matchLogList.getChildAt(i);
            top = matchLogList.getY() + item.getY() - scrollY;
            bottom = top + itemHeight;
            scale = 1f;
            if(bottom < 0){
                //the item is above the screen
                scale = .5f;
            }else if(top < topBottomMargin){
                //the item is in the top quarter
                scale = .5f + (scalePerPixel * bottom);
            }else if(top > Main.heightPixels){
                //the item is below the screen
                scale = .5f;
            }else if(bottom > Main.heightPixels - topBottomMargin){
                //the item is in the bottom quarter
                scale = .5f + (scalePerPixel * (Main.heightPixels - top));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }
}