/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Utils{
    private static final SimpleDateFormat prettyTime_format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
    static String prettyTime(long timestamp){return prettyTime_format.format(new Date(timestamp));}
    static String prettyTime(){return prettyTime_format.format(new Date());}

    static String prettyTimer(int secs){
        String minus = "";
        if(secs < 0){
            secs -= secs * 2;
            minus = "-";
        }
        long minutes = Math.floorDiv(secs, 60);
        secs %= 60;
        if(secs < 10){
            return minus + minutes + ":0" + secs;
        }else{
            return minus + minutes + ":" + secs;
        }
    }

    static String replacementString(Context context, MatchData.Event event){
        if(event.who_enter > 0 || event.who_leave > 0)
            return " " + context.getString(
                    R.string.replaced,
                    event.who_enter == 0 ? "?" : event.who_enter,
                    event.who_leave == 0 ? "?" : event.who_leave
            );
        return "";
    }
}
