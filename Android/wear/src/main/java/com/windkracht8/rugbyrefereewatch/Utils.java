package com.windkracht8.rugbyrefereewatch;

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

}
