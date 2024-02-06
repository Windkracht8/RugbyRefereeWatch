package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class Correct extends ScrollView{
    private MatchData match;
    private LinearLayout correctList;

    private boolean itemHeightInit;
    private int itemHeight = 200;
    private int topBottomMargin;
    private float scalePerPixel;

    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_correct, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.correct, this, true);
    }
    public void show(Main main, MatchData match){
        this.match = match;
        correctList = findViewById(R.id.correctList);
        for(int i = correctList.getChildCount(); i > 0; i--){
            correctList.removeViewAt(i-1);
        }
        for(int i = match.events.size(); i > 0; i--){
            MatchData.event event_data = match.events.get(i-1);
            if(!event_data.what.equals("TRY") &&
                    !event_data.what.equals("CONVERSION") &&
                    !event_data.what.equals("PENALTY TRY") &&
                    !event_data.what.equals("PENALTY") &&
                    !event_data.what.equals("GOAL") &&
                    !event_data.what.equals("YELLOW CARD") &&
                    !event_data.what.equals("RED CARD")
            ){
                continue;
            }
            Event event_ui = new Event(getContext(), event_data);
            correctList.addView(event_ui);
            event_ui.setOnClickListener(this::eventClicked);
            main.addOnTouch(event_ui);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
        findViewById(R.id.svCorrect).requestFocus();
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(!Main.isScreenRound || itemHeightInit) return;
            if(correctList.getChildCount() == 0) return;
            itemHeightInit = true;
            itemHeight = correctList.getChildAt(0).getHeight();
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
        for(int i=0; i<correctList.getChildCount(); i++){
            View item = correctList.getChildAt(i);
            top = correctList.getY() + item.getY() - scrollY;
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
    private void eventClicked(View v){
        if(Main.draggingEnded+100 > Main.getCurrentTimestamp()) return;
        Event event_ui = (Event) v;
        match.removeEvent(event_ui.event);
        setVisibility(View.GONE);
        performClick();
    }
}
