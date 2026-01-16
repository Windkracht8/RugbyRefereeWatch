/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
