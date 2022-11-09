package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;

public class translator {
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
    public static String getEventTypeLocal(Context context, String eventType_system){
        String[] eventTypesLocal = context.getResources().getStringArray(R.array.eventTypes);
        String[] eventTypes_system = context.getResources().getStringArray(R.array.eventTypes_system);
        for(int i=0; i<eventTypes_system.length; i++){
            if(eventTypes_system[i].equals(eventType_system)){
                return eventTypesLocal[i];
            }
        }

        if(eventType_system.startsWith("Time off")) {
            return context.getString(R.string.Time_off);
        }else if(eventType_system.startsWith("Resume time")) {
            return context.getString(R.string.Resume_time);
        }else if(eventType_system.startsWith("Start first half")) {
            return context.getString(R.string.start_first_half);
        }else if(eventType_system.startsWith("Start second half")) {
            return context.getString(R.string.start_second_half);
        }else if(eventType_system.startsWith("Result first half")){
            return context.getString(R.string.result_first_half)+eventType_system.substring(17);
        }else if(eventType_system.startsWith("Result second half")){
            return context.getString(R.string.result_second_half)+eventType_system.substring(18);
        }else if(eventType_system.startsWith("Start period")){
            return context.getString(R.string.start_period)+eventType_system.substring(12);
        }else if(eventType_system.startsWith("Result period")){
            return context.getString(R.string.result_period)+eventType_system.substring(13);
        }else if(eventType_system.startsWith("Start extra time")){
            return context.getString(R.string.start_extra)+eventType_system.substring(16);
        }else if(eventType_system.startsWith("Result extra time")){
            return context.getString(R.string.result_extra)+eventType_system.substring(17);
        }

        Log.e(MainActivity.RRW_LOG_TAG, "translator.getEventTypeLocal unknown value: " + eventType_system);
        return "";
    }
    public static String getTeamColorSystem(Context context, String teamColor){
        String[] teamColors = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColors_system = context.getResources().getStringArray(R.array.teamColors_system);
        for(int i=0; i<teamColors.length; i++){
            if(teamColors[i].equals(teamColor)){
                return teamColors_system[i];
            }
        }
        Log.e(MainActivity.RRW_LOG_TAG, "translator.getTeamColorSystem not found: " + teamColor);
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
        Log.e(MainActivity.RRW_LOG_TAG, "translator.getTeamColorLocal not found: " + teamColor_system);
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
