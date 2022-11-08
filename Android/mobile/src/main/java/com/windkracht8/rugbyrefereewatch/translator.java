package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Spinner;

import java.util.Locale;

public class translator {
    public static String getMatchTypeSystem(Context context, int index, String fallback){
        if(index<=5){
            String[] matchTypesSystem = getDefaultArray(context, R.array.matchTypes);
            return matchTypesSystem[index];
        }
        return fallback;
    }
    public static void setMatchTypeSpin(Context context, Spinner spin, String matchTypeSystem){
        //First look in the standard match types
        String[] matchTypesSystem = getDefaultArray(context, R.array.matchTypes);
        for(int i=0; i<matchTypesSystem.length; i++){
            if(matchTypesSystem[i].equals(matchTypeSystem)){
                spin.setSelection(i);
                return;
            }
        }
        //Not found, loop the spinner to look for custom match type
        setSpinner(spin, matchTypeSystem, 5);
    }
    public static String getEventTypeLocal(Context context, String eventTypeSystem){
        String[] eventTypesLocal = context.getResources().getStringArray(R.array.eventTypes);
        String[] eventTypesSystem = getDefaultArray(context, R.array.eventTypes);
        for(int i=0; i<eventTypesSystem.length; i++){
            if(eventTypesSystem[i].equals(eventTypeSystem)){
                return eventTypesLocal[i];
            }
        }

        if(eventTypeSystem.startsWith("Time off")) {
            return context.getString(R.string.Time_off);
        }else if(eventTypeSystem.startsWith("Resume time")) {
            return context.getString(R.string.Resume_time);
        }else if(eventTypeSystem.startsWith("Start first half")) {
            return context.getString(R.string.start_first_half);
        }else if(eventTypeSystem.startsWith("Start second half")) {
            return context.getString(R.string.start_second_half);
        }else if(eventTypeSystem.startsWith("Result first half")){
            return context.getString(R.string.result_first_half)+eventTypeSystem.substring(17);
        }else if(eventTypeSystem.startsWith("Result second half")){
            return context.getString(R.string.result_second_half)+eventTypeSystem.substring(18);
        }else if(eventTypeSystem.startsWith("Start period")){
            return context.getString(R.string.start_period)+eventTypeSystem.substring(12);
        }else if(eventTypeSystem.startsWith("Result period")){
            return context.getString(R.string.result_period)+eventTypeSystem.substring(13);
        }else if(eventTypeSystem.startsWith("Start extra time")){
            return context.getString(R.string.start_extra)+eventTypeSystem.substring(16);
        }else if(eventTypeSystem.startsWith("Result extra time")){
            return context.getString(R.string.result_extra)+eventTypeSystem.substring(17);
        }

        Log.e(MainActivity.RRW_LOG_TAG, "translator.getEventTypeLocal unknown value: " + eventTypeSystem);
        return "";
    }
    public static String getTeamColorSystem(Context context, String teamColorLocal){
        String[] teamColorsLocal = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColorsSystem = getDefaultArray(context, R.array.teamColors);
        for(int i=0; i<teamColorsLocal.length; i++){
            if(teamColorsLocal[i].equals(teamColorLocal)){
                return teamColorsSystem[i];
            }
        }
        Log.e(MainActivity.RRW_LOG_TAG, "translator.getTeamColorSystem not found: " + teamColorLocal);
        return teamColorsSystem[0];
    }
    public static String getTeamColorLocal(Context context, String teamColorSystem){
        String[] teamColorsLocal = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColorsSystem = getDefaultArray(context, R.array.teamColors);
        for(int i=0; i<teamColorsSystem.length; i++){
            if(teamColorsSystem[i].equals(teamColorSystem)){
                return teamColorsLocal[i];
            }
        }
        Log.e(MainActivity.RRW_LOG_TAG, "translator.getTeamColorLocal not found: " + teamColorSystem);
        return teamColorsLocal[0];
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
    private static String[] getDefaultArray(Context context, int resId){
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(new Locale("default"));
        return context.createConfigurationContext(config).getResources().getStringArray(resId);
    }
}
