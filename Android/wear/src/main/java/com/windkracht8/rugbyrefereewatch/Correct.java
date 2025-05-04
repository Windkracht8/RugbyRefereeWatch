package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Correct extends ScrollView{
    private final LinearLayout llCorrectItems;

    private static float scalePerPixel;
    private static float bottom_quarter;
    private static float below_screen;

    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.correct, this, true);
        llCorrectItems = findViewById(R.id.llCorrectItems);
    }
    void show(Main main){
        llCorrectItems.removeAllViews();
        for(int i = Main.match.events.size(); i > 0; i--){
            MatchData.Event event_data = Main.match.events.get(i-1);
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
            addItem(main, event_data);
        }

        fullScroll(View.FOCUS_UP);
        setVisibility(View.VISIBLE);
        animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
        requestFocus();
    }
    void onCreateMain(){
        if(!Main.isScreenRound) return;
        int item_height = getResources().getDimensionPixelSize(R.dimen.item_height);
        bottom_quarter = Main.vh75 - item_height;
        below_screen = Main.heightPixels - item_height;
        scalePerPixel = 0.2f / Main.vh25;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                if(llCorrectItems.getChildCount() > 0 && llCorrectItems.getChildAt(0).getHeight() > 0){
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    scaleItems(0);
                    setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
                }
            }
        });

        llCorrectItems.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
        findViewById(R.id.correctLabel).setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
    }

    private void addItem(Main main, MatchData.Event event){
        TextView item = new TextView(main, null, 0, R.style.textView_item_single);
        String text = Utils.prettyTimer(event.timer) + " " + Translator.getEventTypeLocal(main, event.what);
        if(event.team != null){
            text += " " + Translator.getTeamLocal(main, event.team);
            if(event.who > 0){
                text += " " + event.who;
            }
        }
        item.setText(text);

        item.setOnClickListener(v->{
            if(Main.draggingEnded+100 > System.currentTimeMillis()) return;
            if(event.deleted){
                Main.match.undeleteEvent(event);
            }else{
                Main.match.removeEvent(event);
                if(Main.match.events.indexOf(event) == Main.match.events.size()-1){
                    if(event.team.equals(MatchData.HOME_ID)){
                        main.kickClockHomeHide();
                    }else{
                        main.kickClockAwayHide();
                    }
                }
            }
            setVisibility(View.GONE);
            performClick();
        });
        if(event.deleted) item.setPaintFlags(item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        llCorrectItems.addView(item);
        main.addOnTouch(item);
    }
    private void scaleItems(int scrollY){
        for(int i = 0; i < llCorrectItems.getChildCount(); i++){
            View item = llCorrectItems.getChildAt(i);
            float top = (llCorrectItems.getY() + item.getY()) - scrollY;
            float scale = 1.0f;
            if(top < 0){
                scale = 0.8f;
            }else if(top < Main.vh25){
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                scale = 0.8f;
            }else if(top > bottom_quarter){
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }

}
