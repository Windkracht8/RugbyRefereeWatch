package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class ConfItem extends LinearLayout{
    enum ConfItemType{
        COLOR_HOME, COLOR_AWAY, MATCH_TYPE, MATCH_TYPE_DETAILS,
        PERIOD_TIME, PERIOD_COUNT, SINBIN, POINTS_TRY, POINTS_CON, POINTS_GOAL,
        CLOCK_PK, CLOCK_CON, CLOCK_RESTART,
        SCREEN_ON, TIMER_TYPE, RECORD_PLAYER, RECORD_PENS, DELAY_END, HELP
    }
    static final List<ConfItemType> confCustomItemTypes = List.of(
            ConfItemType.PERIOD_TIME, ConfItemType.PERIOD_COUNT, ConfItemType.SINBIN,
            ConfItemType.POINTS_TRY, ConfItemType.POINTS_CON, ConfItemType.POINTS_GOAL,
            ConfItemType.CLOCK_PK, ConfItemType.CLOCK_CON, ConfItemType.CLOCK_RESTART
    );
    private final ConfItemType type;
    private final TextView confItemValue;
    ConfItem(Context context, ConfItemType type){
        super(context);
        this.type = type;
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.conf_item, this, true);
        TextView confItemName = findViewById(R.id.confItemName);
        confItemName.setText(context.getString(getConfItemName(type)));
        confItemValue = findViewById(R.id.confItemValue);
        if(type == ConfItemType.HELP || type == ConfItemType.MATCH_TYPE_DETAILS) confItemValue.setVisibility(View.GONE);
        updateValue();
    }
    static int getConfItemName(ConfItemType type){
        return switch(type){
            case COLOR_HOME -> R.string.color_home;
            case COLOR_AWAY -> R.string.color_away;
            case MATCH_TYPE -> R.string.match_type;
            case MATCH_TYPE_DETAILS -> R.string.match_type_details;
            case PERIOD_TIME -> R.string.period_time;
            case PERIOD_COUNT -> R.string.period_count;
            case SINBIN -> R.string.sinbin;
            case POINTS_TRY -> R.string.points_try;
            case POINTS_CON -> R.string.points_con;
            case POINTS_GOAL -> R.string.points_goal;
            case CLOCK_PK -> R.string.clock_pk;
            case CLOCK_CON -> R.string.clock_con;
            case CLOCK_RESTART -> R.string.clock_restart;
            case SCREEN_ON -> R.string.screen;
            case TIMER_TYPE -> R.string.timer_type;
            case RECORD_PLAYER -> R.string.record_player;
            case RECORD_PENS -> R.string.record_pens;
            case DELAY_END -> R.string.delay_end;
            //case HELP -> R.string.help;
            default -> R.string.help;//Bug in JDK 21, default must be defined
        };
    }

    void updateValue(){//Thread: Always on UI thread
        switch(type){
            case COLOR_HOME:
                confItemValue.setText(Translator.getTeamColorLocal(getContext(), Main.match.home.color));
                break;
            case COLOR_AWAY:
                confItemValue.setText(Translator.getTeamColorLocal(getContext(), Main.match.away.color));
                break;
            case MATCH_TYPE:
                confItemValue.setText(Main.match.match_type);
                break;
            case PERIOD_TIME:
                confItemValue.setText(String.valueOf(Main.match.period_time));
                Main.match.match_type = "custom";
                break;
            case PERIOD_COUNT:
                confItemValue.setText(String.valueOf(Main.match.period_count));
                Main.match.match_type = "custom";
                break;
            case SINBIN:
                confItemValue.setText(String.valueOf(Main.match.sinbin));
                Main.match.match_type = "custom";
                break;
            case POINTS_TRY:
                confItemValue.setText(String.valueOf(Main.match.points_try));
                Main.match.match_type = "custom";
                break;
            case POINTS_CON:
                confItemValue.setText(String.valueOf(Main.match.points_con));
                Main.match.match_type = "custom";
                break;
            case POINTS_GOAL:
                confItemValue.setText(String.valueOf(Main.match.points_goal));
                Main.match.match_type = "custom";
                break;
            case CLOCK_PK:
                confItemValue.setText(String.valueOf(Main.match.clock_pk));
                Main.match.match_type = "custom";
                break;
            case CLOCK_CON:
                confItemValue.setText(String.valueOf(Main.match.clock_con));
                Main.match.match_type = "custom";
                break;
            case CLOCK_RESTART:
                confItemValue.setText(String.valueOf(Main.match.clock_restart));
                Main.match.match_type = "custom";
                break;
            case SCREEN_ON:
                if(Main.screen_on)
                    confItemValue.setText(R.string.keep_on);
                else
                    confItemValue.setText(R.string.auto_off);
                break;
            case TIMER_TYPE:
                if(Main.timer_type_period == Main.TIMER_TYPE_DOWN)
                    confItemValue.setText(R.string.timer_type_down);
                else
                    confItemValue.setText(R.string.timer_type_up);
                break;
            case RECORD_PLAYER:
                if(Main.record_player)
                    confItemValue.setText(R.string.on);
                else
                    confItemValue.setText(R.string.off);
                break;
            case RECORD_PENS:
                if(Main.record_pens)
                    confItemValue.setText(R.string.on);
                else
                    confItemValue.setText(R.string.off);
                break;
            case DELAY_END:
                if(Main.delay_end)
                    confItemValue.setText(R.string.on);
                else
                    confItemValue.setText(R.string.off);
                break;
        }
    }
}

