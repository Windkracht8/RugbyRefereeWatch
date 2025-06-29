/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class PlayerNo extends ConstraintLayout{
    private final TextView player_no;
    private final TextView b_back;
    private int player_no_int = 0;
    private MatchData.Event event;
    private MatchData.Sinbin sinbin;
    public PlayerNo(@NonNull Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.player_no, this, true);
        player_no = findViewById(R.id.player_no);
        findViewById(R.id.b_0).setOnClickListener(v->addNumber(0));
        findViewById(R.id.b_1).setOnClickListener(v->addNumber(1));
        findViewById(R.id.b_2).setOnClickListener(v->addNumber(2));
        findViewById(R.id.b_3).setOnClickListener(v->addNumber(3));
        findViewById(R.id.b_4).setOnClickListener(v->addNumber(4));
        findViewById(R.id.b_5).setOnClickListener(v->addNumber(5));
        findViewById(R.id.b_6).setOnClickListener(v->addNumber(6));
        findViewById(R.id.b_7).setOnClickListener(v->addNumber(7));
        findViewById(R.id.b_8).setOnClickListener(v->addNumber(8));
        findViewById(R.id.b_9).setOnClickListener(v->addNumber(9));
        b_back = findViewById(R.id.b_back);
        b_back.setOnClickListener(v->{
            player_no_int = Math.floorDiv(player_no_int, 10);
            showNumber();
        });
    }
    void onCreateMain(Main main){
        findViewById(R.id.b_done).setOnClickListener(v->{
            event.who = player_no_int;
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
            setVisibility(View.GONE);
        });
        ((LayoutParams)findViewById(R.id.b_0).getLayoutParams()).setMargins(Main.vh10, 0, Main.vh10, 0);
    }
    void show(MatchData.Event event, MatchData.Sinbin sinbin){
        this.event = event;
        this.sinbin = sinbin;
        player_no_int = 0;
        showNumber();
        setVisibility(View.VISIBLE);
    }
    private void addNumber(int number){
        player_no_int = player_no_int * 10 + number;
        showNumber();
    }
    private void showNumber(){
        if(player_no_int == 0){
            player_no.setText(R.string.no0);
            player_no.setTextColor(getResources().getColor(R.color.hint, null));
            b_back.setTextColor(getResources().getColor(R.color.hint, null));
        }else{
            player_no.setText(String.valueOf(player_no_int));
            player_no.setTextColor(getResources().getColor(R.color.white, null));
            b_back.setTextColor(getResources().getColor(R.color.white, null));
        }
    }
}
