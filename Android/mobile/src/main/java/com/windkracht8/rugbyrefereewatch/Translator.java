package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;

class Translator{
    public static String getMatchTypeSystem(Context context, int index, String fallback){
        if(index<=5){
            String[] matchTypes_system = context.getResources().getStringArray(R.array.matchTypes_system);
            return matchTypes_system[index];
        }
        return fallback;
    }
    public static void setMatchTypeSpin(Context context, Spinner spin, String matchType_system){
        //First look in the standard match types
        String[] matchTypes_system = context.getResources().getStringArray(R.array.matchTypes_system);
        for(int i=0; i<matchTypes_system.length; i++){
            if(matchTypes_system[i].equals(matchType_system)){
                spin.setSelection(i);
                return;
            }
        }
        //Not found, loop the spinner to look for custom match type
        setSpinner(spin, matchType_system, 5);
    }
    public static String getEventTypeLocal(Context context, String eventType_system, int period, int period_count){
        String[] eventTypesLocal = context.getResources().getStringArray(R.array.eventTypes);
        String[] eventTypes_system = context.getResources().getStringArray(R.array.eventTypes_system);
        for(int i=0; i<eventTypes_system.length; i++){
            if(eventTypes_system[i].equals(eventType_system)){
                return eventTypesLocal[i];
            }
        }

        switch(eventType_system){
            case "TIME OFF":
                return context.getString(R.string.Time_off);
            case "RESUME":
                return context.getString(R.string.Resume_time);
            case "START":
                return context.getString(R.string.Start) + " " + getPeriodName(context, period, period_count);
            case "END":
                return context.getString(R.string.Result) + " " + getPeriodName(context, period, period_count);
        }
        Log.e(Main.LOG_TAG, "translator.getEventTypeLocal unknown value: " + eventType_system);
        return "";
    }
    private static String getPeriodName(Context context, int period, int period_count){
        if(period > period_count){
            if(period == period_count+1){
                return context.getString(R.string.extra_time);
            }else{
                return context.getString(R.string.extra_time) + " " + (period - period_count);
            }
        }else if(period_count == 2){
            switch(period){
                case 1:
                    return context.getString(R.string.first_half);
                case 2:
                    return context.getString(R.string.second_half);
            }
        }else{
            switch(period){
                case 1:
                    return context.getString(R.string._1st);
                case 2:
                    return context.getString(R.string._2nd);
                case 3:
                    return context.getString(R.string._3rd);
                case 4:
                    return context.getString(R.string._4th);
            }
        }
        return context.getString(R.string.period) + " " + period;
    }
    public static String getTeamColorSystem(Context context, String teamColor){
        String[] teamColors = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColors_system = context.getResources().getStringArray(R.array.teamColors_system);
        for(int i=0; i<teamColors.length; i++){
            if(teamColors[i].equals(teamColor)){
                return teamColors_system[i];
            }
        }
        Log.e(Main.LOG_TAG, "translator.getTeamColorSystem not found: " + teamColor);
        return teamColors_system[0];
    }
    public static String getTeamColorLocal(Context context, String teamColor_system){
        String[] teamColors = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColors_system = context.getResources().getStringArray(R.array.teamColors_system);
        for(int i=0; i<teamColors_system.length; i++){
            if(teamColors_system[i].equals(teamColor_system)){
                return teamColors[i];
            }
        }
        Log.e(Main.LOG_TAG, "translator.getTeamColorLocal not found: " + teamColor_system);
        return teamColors[0];
    }

    public static void setTeamColorSpin(Context context, Spinner spin, String teamColor){
        setSpinner(spin, getTeamColorLocal(context, teamColor), 0);
    }
    private static void setSpinner(Spinner spin, String str, int def){
        for(int i=0;i<spin.getCount();i++){
            if(spin.getItemAtPosition(i).equals(str)){
                spin.setSelection(i);
                return;
            }
        }
        spin.setSelection(def);
    }
}
