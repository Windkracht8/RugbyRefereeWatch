package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;

public class translator {
    public static String getMatchTypeSystem(Context context, int index, String fallback){
        if(index<=5){
            String[] matchTypes_system = context.getResources().getStringArray(R.array.matchTypes_system);
            return matchTypes_system[index];
        }
        return fallback;
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
    public static String getEventTypeLocal(Context context, String eventType_system){
        String[] eventTypesLocal = context.getResources().getStringArray(R.array.eventTypes);
        String[] eventTypes_system = context.getResources().getStringArray(R.array.eventTypes_system);
        for (int i = 0; i < eventTypes_system.length; i++) {
            if (eventTypes_system[i].equals(eventType_system)) {
                return eventTypesLocal[i];
            }
        }
        return "";
    }
    public static String getTeamLocal(Context context, String team_system){
        if(team_system.equals("home")) return context.getString(R.string.home);
        if(team_system.equals("away")) return context.getString(R.string.away);
        return team_system;
    }
}
