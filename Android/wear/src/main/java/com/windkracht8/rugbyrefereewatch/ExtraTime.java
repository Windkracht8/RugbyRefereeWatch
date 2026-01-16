/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExtraTime extends LinearLayout{
    public ExtraTime(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.extra_time, this, true);
    }
    void onCreateMain(Main main){
        TextView extra_time_up = findViewById(R.id.extra_time_up);
        TextView extra_time_2min = findViewById(R.id.extra_time_2min);
        TextView extra_time_5min = findViewById(R.id.extra_time_5min);
        TextView extra_time_10min = findViewById(R.id.extra_time_10min);
        extra_time_up.setOnClickListener(v->main.extraTimeChange(0));
        extra_time_2min.setOnClickListener(v->main.extraTimeChange(2));
        extra_time_5min.setOnClickListener(v->main.extraTimeChange(5));
        extra_time_10min.setOnClickListener(v->main.extraTimeChange(10));
        if(getResources().getConfiguration().fontScale > 1.1){
            extra_time_up.setIncludeFontPadding(false);
            extra_time_2min.setIncludeFontPadding(false);
            extra_time_5min.setIncludeFontPadding(false);
            extra_time_10min.setIncludeFontPadding(false);
        }
    }

}
