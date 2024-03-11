package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Correct extends ScrollView{
    private LinearLayout llCorrectItems;

    private static int itemHeight;
    private static float scalePerPixel = 0;
    private static float bottom_quarter;
    private static float below_screen;

    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.correct, this, true);
    }
    void show(Main main){
        llCorrectItems = findViewById(R.id.llCorrectItems);
        for(int i = llCorrectItems.getChildCount(); i > 0; i--){
            llCorrectItems.removeViewAt(i-1);
        }
        for(int i = Main.match.events.size(); i > 0; i--){
            MatchData.event event_data = Main.match.events.get(i-1);
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
            addNewItem(main, event_data);
        }

        if(Main.isScreenRound){
            getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(scalePerPixel > 0 || llCorrectItems.getChildCount() == 0) return;
                itemHeight = llCorrectItems.getChildAt(0).getHeight();
                bottom_quarter = Main.vh75 - itemHeight;
                below_screen = Main.heightPixels - itemHeight;
                scalePerPixel = 0.2f / Main.vh25;
                scaleItems(0);
                setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
            });
        }
        fullScroll(View.FOCUS_UP);
        setVisibility(View.VISIBLE);
        animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
        requestFocus();
    }
    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(int i = 0; i< llCorrectItems.getChildCount(); i++){
            View item = llCorrectItems.getChildAt(i);
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
    private void addNewItem(Main main, MatchData.event event){
        TextView item = new TextView(main, null, 0, R.style.textView_item);
        String text = Main.prettyTimer(event.timer) + " " + Translator.getEventTypeLocal(main, event.what);
        if(event.team != null){
            text += " " + Translator.getTeamLocal(main, event.team);
            if(event.who > 0){
                text += " " + event.who;
            }
        }
        item.setText(text);

        item.setOnClickListener(v->{
            if(Main.draggingEnded+100 > Main.getCurrentTimestamp()) return;
            Main.match.removeEvent(event);
            setVisibility(View.GONE);
            performClick();
        });
        llCorrectItems.addView(item);
        main.addOnTouch(item);
    }
}
