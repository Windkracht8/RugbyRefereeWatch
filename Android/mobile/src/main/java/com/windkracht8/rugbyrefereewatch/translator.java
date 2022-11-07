package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;

import java.util.HashMap;

public class translator {
    private static final HashMap<String, String> teamColorsSystem = new HashMap<>();

    public static void init(Context context){
        teamColorsSystem.put(context.getString(R.string.black), "black");
        teamColorsSystem.put(context.getString(R.string.blue), "blue");
        teamColorsSystem.put(context.getString(R.string.brown), "brown");
        teamColorsSystem.put(context.getString(R.string.gold), "gold");
        teamColorsSystem.put(context.getString(R.string.green), "green");
        teamColorsSystem.put(context.getString(R.string.orange), "orange");
        teamColorsSystem.put(context.getString(R.string.pink), "pink");
        teamColorsSystem.put(context.getString(R.string.purple), "purple");
        teamColorsSystem.put(context.getString(R.string.red), "red");
        teamColorsSystem.put(context.getString(R.string.white), "white");
    }
    public static String getMatchTypeSystem(int index){
        switch(index){
            case 0:
                return "15s";
            case 1:
                return "10s";
            case 2:
                return "7s";
            case 3:
                return "beach 7s";
            case 4:
                return "beach 5s";
            case 5:
                return "custom";
        }
        Log.e(MainActivity.RRW_LOG_TAG, "getMatchTypeSystem index out of bound: " + index);
        return "";
    }
    public static void setMatchType(Spinner spin, String matchTypeSystem){
        switch(matchTypeSystem){
            case "15s":
                spin.setSelection(0);
                break;
            case "10s":
                spin.setSelection(1);
                break;
            case "7s":
                spin.setSelection(2);
                break;
            case "beach 7s":
                spin.setSelection(3);
                break;
            case "beach 5s":
                spin.setSelection(4);
                break;
            case "custom":
                spin.setSelection(5);
                break;
            default:
                setSpinner(spin, matchTypeSystem, 5);
        }
    }
    public static String getEventTypeLocal(Context context, String eventTypeSystem){
        switch(eventTypeSystem){
            case "TRY":
                return context.getString(R.string.TRY);
            case "CONVERSION":
                return context.getString(R.string.CONVERSION);
            case "PENALTY TRY":
                return context.getString(R.string.PENALTY_TRY);
            case "GOAL":
                return context.getString(R.string.GOAL);
            case "PENALTY GOAL":
                return context.getString(R.string.PENALTY_GOAL);
            case "DROP GOAL":
                return context.getString(R.string.DROP_GOAL);
            case "YELLOW CARD":
                return context.getString(R.string.YELLOW_CARD);
            case "RED CARD":
                return context.getString(R.string.RED_CARD);
            case "PENALTY":
                return context.getString(R.string.PENALTY);
            case "Time off":
                return context.getString(R.string.Time_off);
            case "Resume time":
                return context.getString(R.string.Resume_time);
            case "Start first half":
                return context.getString(R.string.start_first_half);
            case "Start second half":
                return context.getString(R.string.start_second_half);
            default:
                if(eventTypeSystem.startsWith("Result first half")){
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
        }
        Log.e(MainActivity.RRW_LOG_TAG, "getEventTypeLocal unknown value: " + eventTypeSystem);
        return "";
    }
    public static String getTeamColorSystem(String teamColorSystem){return teamColorsSystem.get(teamColorSystem);}
    public static String getTeamColorLocal(Context context, String teamColorSystem){
        switch(teamColorSystem){
            case "black":
                return context.getString(R.string.black);
            case "blue":
                return context.getString(R.string.blue);
            case "brown":
                return context.getString(R.string.brown);
            case "gold":
                return context.getString(R.string.gold);
            case "green":
                return context.getString(R.string.green);
            case "orange":
                return context.getString(R.string.orange);
            case "pink":
                return context.getString(R.string.pink);
            case "purple":
                return context.getString(R.string.purple);
            case "red":
                return context.getString(R.string.red);
            case "white":
                return context.getString(R.string.white);
        }
        Log.e(MainActivity.RRW_LOG_TAG, "getTeamColorLocal unknown value: " + teamColorSystem);
        return "";
    }
    public static void setTeamColor(Context context, Spinner spin, String teamColor){
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
