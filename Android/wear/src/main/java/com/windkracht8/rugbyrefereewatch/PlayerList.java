/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import static com.windkracht8.rugbyrefereewatch.PlayerNo.TYPE_ENTER;
import static com.windkracht8.rugbyrefereewatch.PlayerNo.TYPE_LEAVE;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;

public class PlayerList extends ConstraintLayout {
    private int type = 0;
    private final TextView b_keypad;
    private final TextView title;
    private final LinearLayout list;
    private MatchData.Event event;
    private MatchData.Sinbin sinbin;
    public PlayerList(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.player_list, this, true);
        b_keypad = findViewById(R.id.b_keypad);
        title = findViewById(R.id.title);
        list = findViewById(R.id.list);
    }
    void onCreateMain(Main main){
        /*findViewById(R.id.b_done).setOnClickListener(v->{
            switch(type){
                case TYPE_LEAVE:
                    event.who_leave = player_no_int;
                    break;
                case TYPE_ENTER:
                    event.who_enter = player_no_int;
                    break;
                default:
                    event.who = player_no_int;
                    break;
            }

            if(event.what.equals("YELLOW CARD") && Main.match.alreadyHasYellow(event)){
                main.confirm_label.setText(R.string.confirm_second_yellow);
                main.confirm_label.setLines(2);
                main.confirm_no.setOnClickListener(n->main.confirm.setVisibility(View.GONE));
                main.confirm_yes.setOnClickListener(y->{
                    Main.match.convertYellowToRed(event);
                    main.updateSinbins();
                    main.confirm.setVisibility(View.GONE);
                });
                main.confirm.setVisibility(View.VISIBLE);
            }
            if(sinbin != null){
                sinbin.who = player_no_int;
                main.updateSinbins();
            }
            if(type == TYPE_LEAVE) show(event, TYPE_ENTER, null);
            else setVisibility(View.GONE);
        });*/
    }
    void show(MatchData.Event event, int type, MatchData.Sinbin sinbin, ArrayList<MatchData.Player> players){
        this.event = event;
        this.type = type;
        this.sinbin = sinbin;

        list.removeAllViews();

        for(MatchData.Player player : players){
            list.addView(new Player(getContext(), player));
        }

        setVisibility(View.VISIBLE);
    }
}
