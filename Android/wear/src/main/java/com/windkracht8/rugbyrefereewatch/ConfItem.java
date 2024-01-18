package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ConfItem extends LinearLayout{
    enum ConfItemType {
        COLOR_HOME, COLOR_AWAY, MATCH_TYPE, PERIOD_TIME, PERIOD_COUNT, SINBIN, POINTS_TRY,
        POINTS_CON, POINTS_GOAL, SCREEN_ON, TIMER_TYPE, RECORD_PLAYER, RECORD_PENS, BLUETOOTH, HELP
    }
    public ConfItemType type;
    private TextView confItemName;
    private TextView confItemValue;
    public ConfItem(Context context, AttributeSet attrs){super(context, attrs);}
    public ConfItem(Context context, AttributeSet attrs, ConfItemType type){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show();return;}
        inflater.inflate(R.layout.conf_item, this, true);

        confItemName = findViewById(R.id.confItemName);
        confItemValue = findViewById(R.id.confItemValue);

        this.type = type;
        confItemName.setText(context.getString(getConfItemName(type)));
        if(type == ConfItemType.HELP){
            confItemValue.setVisibility(View.GONE);
        }else{
            confItemValue.setContentDescription(context.getString(R.string.confItemValue_desc) + type);
        }
    }
    public static int getConfItemName(ConfItemType type){
        switch(type){
            case COLOR_HOME:
                return R.string.color_home;
            case COLOR_AWAY:
                return R.string.color_away;
            case MATCH_TYPE:
                return R.string.match_type;
            case PERIOD_TIME:
                return R.string.period_time;
            case PERIOD_COUNT:
                return R.string.period_count;
            case SINBIN:
                return R.string.sinbin;
            case POINTS_TRY:
                return R.string.points_try;
            case POINTS_CON:
                return R.string.points_con;
            case POINTS_GOAL:
                return R.string.points_goal;
            case SCREEN_ON:
                return R.string.screen_on;
            case TIMER_TYPE:
                return R.string.timer_type;
            case RECORD_PLAYER:
                return R.string.record_player;
            case RECORD_PENS:
                return R.string.record_pens;
            case BLUETOOTH:
                return R.string.bluetooth;
            case HELP:
                return R.string.help;
        }
        return R.string.fail_oops;
    }

    public void addOnTouch(Main main){
        main.addOnTouch(this);
        main.addOnTouch(confItemName);
        main.addOnTouch(confItemValue);
    }
    private boolean hideForMatchType(){
        if(Main.match.match_type.equals("custom")){
            this.setVisibility(View.VISIBLE);
            return false;
        }else{
            this.setVisibility(View.GONE);
            return true;
        }
    }
    public void updateValue(){
        String value = "";
        switch(type){
            case COLOR_HOME:
                value = Translator.getTeamColorLocal(getContext(), Main.match.home.color);
                break;
            case COLOR_AWAY:
                value = Translator.getTeamColorLocal(getContext(), Main.match.away.color);
                break;
            case MATCH_TYPE:
                value = Main.match.match_type;
                break;
            case PERIOD_TIME:
                if(hideForMatchType()) return;
                value += Main.match.period_time;
                break;
            case PERIOD_COUNT:
                if(hideForMatchType()) return;
                value += Main.match.period_count;
                break;
            case SINBIN:
                if(hideForMatchType()) return;
                value += Main.match.sinbin;
                break;
            case POINTS_TRY:
                if(hideForMatchType()) return;
                value += Main.match.points_try;
                break;
            case POINTS_CON:
                if(hideForMatchType()) return;
                value += Main.match.points_con;
                break;
            case POINTS_GOAL:
                if(hideForMatchType()) return;
                value += Main.match.points_goal;
                break;
            case SCREEN_ON:
                value = getContext().getString(Main.screen_on ? R.string.on : R.string.off);
                break;
            case TIMER_TYPE:
                value = getContext().getString(Main.timer_type_period == 1 ? R.string.timer_type_down : R.string.timer_type_up);
                break;
            case RECORD_PLAYER:
                value = getContext().getString(Main.record_player ? R.string.on : R.string.off);
                break;
            case RECORD_PENS:
                value = getContext().getString(Main.record_pens ? R.string.on : R.string.off);
                break;
            case BLUETOOTH:
                value = getContext().getString(Main.bluetooth ? R.string.on : R.string.off);
                break;
            case HELP:
                return;
        }
        confItemValue.setText(value);
    }
}

