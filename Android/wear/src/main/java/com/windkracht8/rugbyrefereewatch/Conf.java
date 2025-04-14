package com.windkracht8.rugbyrefereewatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Conf extends ConfScreen{
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        label.setText(R.string.conf_title);
        for(ConfItem.ConfItemType confItemType : ConfItem.ConfItemType.values()){
            if(ConfItem.confCustomItemTypes.contains(confItemType)) continue;
            ConfItem confItem = new ConfItem(confActivity, confItemType);
            confItem.setOnClickListener(v->onConfItemClick(confItem, confItemType));
            super.addItem(confItem);
        }
        Main.runInBackground(()->FileStore.readCustomMatchTypes(confActivity));
        return rootView;
    }

    private void onConfItemClick(ConfItem confItem, ConfItem.ConfItemType type){
        switch(type){
            case COLOR_HOME:
            case COLOR_AWAY:
            case MATCH_TYPE:
                confActivity.openConfScreen(new ConfSpinner(type));
                break;
            case MATCH_TYPE_DETAILS:
                confActivity.openConfCustomScreen();
                break;
            case SCREEN_ON:
                Main.screen_on = !Main.screen_on;
                confItem.updateValue();
                break;
            case TIMER_TYPE:
                Main.timer_type = Main.timer_type == 1 ? 0 : 1;
                Main.timer_type_period = Main.timer_type;
                confItem.updateValue();
                break;
            case RECORD_PLAYER:
                Main.record_player = !Main.record_player;
                confItem.updateValue();
                break;
            case RECORD_PENS:
                Main.record_pens = !Main.record_pens;
                confItem.updateValue();
                break;
            case DELAY_END:
                Main.delay_end = !Main.delay_end;
                confItem.updateValue();
                break;
            case HELP:
                startActivity(new Intent(confActivity, Help.class));
                confActivity.finish();
                break;
        }
    }
}
