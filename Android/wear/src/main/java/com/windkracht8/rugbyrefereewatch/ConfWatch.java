package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class ConfWatch extends LinearLayout{
    private boolean isInitialized = false;
    private final ArrayList<ConfItem> confItems = new ArrayList<>();

    public ConfWatch(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.conf_watch, this, true);
    }
    void show(Main main){
        if(isInitialized){
            for(ConfItem confItem : confItems) confItem.updateValue();
            setVisibility(View.VISIBLE);
            return;
        }
        isInitialized = true;
        LinearLayout llConfWatch = findViewById(R.id.llConfWatch);
        for(ConfItem.ConfItemType confItemType : new ConfItem.ConfItemType[]{
                ConfItem.ConfItemType.TIMER_TYPE
                ,ConfItem.ConfItemType.RECORD_PENS
                ,ConfItem.ConfItemType.RECORD_PLAYER
                ,ConfItem.ConfItemType.SCREEN_ON
        }){
            ConfItem confItem = new ConfItem(getContext(), confItemType);
            confItem.setOnClickListener(v->onConfItemClick((ConfItem)v, confItemType));
            confItems.add(confItem);
            llConfWatch.addView(confItem);
            main.addOnTouch(confItem);
            confItem.getLayoutParams().height = Main.vh25;
            confItem.updateValue();
        }
        if(Main.isScreenRound){
            confItems.get(0).setPadding(Main.vh15, Main.vh5, Main.vh15, 0);
            confItems.get(confItems.size()-1).setPadding(Main.vh15, 0, Main.vh15, Main.vh5);
        }
        setVisibility(View.VISIBLE);
    }

    private void onConfItemClick(ConfItem confItem, ConfItem.ConfItemType type){
        switch(type){
            case TIMER_TYPE:
                Main.timer_type = Main.timer_type == 1 ? 0 : 1;
                Main.timer_type_period = Main.timer_type;
                confItem.updateValue();
                break;
            case RECORD_PENS:
                Main.record_pens = !Main.record_pens;
                confItem.updateValue();
                break;
            case RECORD_PLAYER:
                Main.record_player = !Main.record_player;
                confItem.updateValue();
                break;
            case SCREEN_ON:
                Main.screen_on = !Main.screen_on;
                confItem.updateValue();
                break;
        }
    }
}
