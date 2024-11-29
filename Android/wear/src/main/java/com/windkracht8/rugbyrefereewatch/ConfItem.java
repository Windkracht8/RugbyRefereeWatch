package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

class ConfItem extends LinearLayout{
    enum ConfItemType {
        COLOR_HOME, COLOR_AWAY, MATCH_TYPE, PERIOD_TIME, PERIOD_COUNT, SINBIN, POINTS_TRY,
        POINTS_CON, POINTS_GOAL, SCREEN_ON, TIMER_TYPE, RECORD_PLAYER, RECORD_PENS, HELP, COMMS_LOG
    }
    private final ConfItemType type;
    private final TextView confItemValue;
    ConfItem(Context context, ConfItemType type){
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.conf_item, this, true);

        TextView confItemName = findViewById(R.id.confItemName);
        confItemValue = findViewById(R.id.confItemValue);

        this.type = type;
        confItemName.setText(context.getString(getConfItemName(type)));
        confItemName.setContentDescription(context.getString(R.string.confItemName_desc) + type);
        if(type == ConfItemType.HELP || type == ConfItemType.COMMS_LOG){
            confItemValue.setVisibility(View.GONE);
        }else{
            confItemValue.setContentDescription(context.getString(R.string.confItemValue_desc) + type);
        }
    }
    static int getConfItemName(ConfItemType type){
        return switch(type){
            case COLOR_HOME -> R.string.color_home;
            case COLOR_AWAY -> R.string.color_away;
            case MATCH_TYPE -> R.string.match_type;
            case PERIOD_TIME -> R.string.period_time;
            case PERIOD_COUNT -> R.string.period_count;
            case SINBIN -> R.string.sinbin;
            case POINTS_TRY -> R.string.points_try;
            case POINTS_CON -> R.string.points_con;
            case POINTS_GOAL -> R.string.points_goal;
            case SCREEN_ON -> R.string.screen_on;
            case TIMER_TYPE -> R.string.timer_type;
            case RECORD_PLAYER -> R.string.record_player;
            case RECORD_PENS -> R.string.record_pens;
            case HELP -> R.string.help;
            case COMMS_LOG -> R.string.commsBTLog;
        };
    }

    private boolean hideForMatchType(){//Thread: Always on UI thread
        if(Main.match.match_type.equals("custom")){
            setVisibility(View.VISIBLE);
            return false;
        }else{
            setVisibility(View.GONE);
            return true;
        }
    }
    void updateValue(){//Thread: Always on UI thread
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
            case HELP:
                return;
        }
        confItemValue.setText(value);
    }
}

