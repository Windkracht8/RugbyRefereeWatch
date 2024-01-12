package com.windkracht8.rugbyrefereewatch;

import static com.windkracht8.rugbyrefereewatch.ConfItem.ConfItemType.*;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConfWatch extends ScrollView{
    private boolean isInitialized = false;
    private final ArrayList<ConfItem> confItems = new ArrayList<>();

    public ConfWatch(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){ Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.conf_watch, this, true);
    }
    public void show(Main main){
        if(isInitialized){
            for(ConfItem confItem : confItems) confItem.updateValue();
            this.setVisibility(View.VISIBLE);
            return;
        }
        isInitialized = true;

        LinearLayout llConfWatch = findViewById(R.id.llConfWatch);

        int ll_in_sc_padding = getResources().getDimensionPixelSize(R.dimen.ll_in_sc_padding);
        int minTouchSize = getResources().getDimensionPixelSize(R.dimen.minTouchSize);
        int confItemHeightText = minTouchSize / 6;
        int confItemHeightTotal = minTouchSize;

        if(Main.heightPixels <= ll_in_sc_padding*2+minTouchSize*4){
            llConfWatch.setPadding(ll_in_sc_padding, 0,ll_in_sc_padding,0);
        }else if(Main.heightPixels > ll_in_sc_padding*2+minTouchSize*4){
            confItemHeightText = (Main.heightPixels - ll_in_sc_padding*2) / 20;
            confItemHeightTotal = (Main.heightPixels - ll_in_sc_padding*2) / 4;
        }

        for(ConfItem.ConfItemType confItemType : new ConfItem.ConfItemType[]{TIMER_TYPE, RECORD_PENS, RECORD_PLAYER, SCREEN_ON}){
            ConfItem confItem = new ConfItem(getContext(), null, confItemType);
            confItem.setOnClickListener(v -> onConfItemClick((ConfItem)v, confItemType));
            confItems.add(confItem);
            llConfWatch.addView(confItem);
            confItem.addOnTouch(main);
            confItem.setHeight(confItemHeightText, confItemHeightTotal);
            confItem.updateValue();
            if(Main.isScreenRound && (confItemType == TIMER_TYPE || confItemType == SCREEN_ON)){
                confItem.setPadding(Main.vh15, 0, Main.vh15, 0);
            }
        }
        this.setVisibility(View.VISIBLE);
        findViewById(R.id.svConfWatch).requestFocus();
    }

    private void onConfItemClick(ConfItem confItem, ConfItem.ConfItemType type){
        switch(type){
            case TIMER_TYPE:
                Main.timer_type_period = Main.timer_type_period == 1 ? 0 : 1;
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
